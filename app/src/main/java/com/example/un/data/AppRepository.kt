package com.example.un.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.example.un.data.local.*
import com.example.un.data.remote.SyncWorker
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AppRepository(
    private val clientDao: ClientDao,
    private val lieuDao: LieuDao,
    private val collegueDao: CollegueDao,
    private val database: FirebaseDatabase,
    private val context: Context
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val clientsRef = database.getReference(AdminConfig.PATH_SHARED_PATIENTS)
    private val lieuxRef = database.getReference("shared/lieux_codes")
    private val colleguesRef = database.getReference(AdminConfig.PATH_SHARED_COLLEGUES)

    private fun showError(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // --- PATIENTS ---
    val allClients: Flow<List<ClientEntity>> = clientDao.getAllVisibleClients()
    fun getClientFlow(id: String): Flow<ClientEntity?> = clientDao.getClientFlow(id)

    suspend fun saveClient(client: ClientEntity) {
        val toSave = client.copy(isSynced = false)
        clientDao.insertOrUpdate(toSave)
        try {
            withTimeout(5000) {
                clientsRef.child(client.id).setValue(toSave.copy(isSynced = true)).await()
                clientDao.markAsSynced(client.id)
                Log.d("AppRepository", "Sync direct Patient réussi: ${client.nom}")
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Sync direct Patient échec: ${e.message}")
            showError("Échec partage Patient: ${e.message}")
            scheduleSync()
        }
    }

    suspend fun softDeleteClient(id: String) {
        val timestamp = System.currentTimeMillis()
        clientDao.softDelete(id, timestamp)
        try {
            clientsRef.child(id).child("isDeleted").setValue(true).await()
            clientsRef.child(id).child("updatedAt").setValue(timestamp).await()
            clientDao.markAsSynced(id)
        } catch (e: Exception) {
            showError("Échec suppression Patient: ${e.message}")
            scheduleSync()
        }
    }

    // --- LIEUX ---
    val allLieux: Flow<List<LieuEntity>> = lieuDao.getAllLieux()
    fun getLieuFlow(id: String): Flow<LieuEntity?> = lieuDao.getLieuFlow(id)

    suspend fun saveLieu(lieu: LieuEntity) {
        val toSave = lieu.copy(isSynced = false)
        lieuDao.insertOrUpdate(toSave)
        try {
            withTimeout(5000) {
                lieuxRef.child(lieu.id).setValue(toSave.copy(isSynced = true)).await()
                lieuDao.markAsSynced(lieu.id)
                Log.d("AppRepository", "Sync direct Lieu réussi: ${lieu.nomLieu}")
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Sync direct Lieu échec: ${e.message}")
            showError("Échec partage Lieu: ${e.message}")
            scheduleSync()
        }
    }

    suspend fun deleteLieu(id: String) {
        lieuDao.deleteById(id)
        try {
            lieuxRef.child(id).removeValue().await()
        } catch (e: Exception) {
            showError("Échec suppression Lieu: ${e.message}")
        }
    }

    // --- COLLEGUES ---
    val allCollegues: Flow<List<CollegueEntity>> = collegueDao.getAllCollegues()
    
    suspend fun saveCollegue(collegue: CollegueEntity) {
        val toSave = collegue.copy(isSynced = false)
        collegueDao.insertOrUpdate(toSave)
        try {
            withTimeout(5000) {
                colleguesRef.child(collegue.id).setValue(toSave.copy(isSynced = true)).await()
                collegueDao.markAsSynced(collegue.id)
                Log.d("AppRepository", "Sync direct Collegue réussi: ${collegue.prenom}")
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Sync direct Collegue échec: ${e.message}")
            showError("Échec partage Profil: ${e.message}")
            scheduleSync()
        }
    }

    suspend fun forceSyncAll() {
        repositoryScope.launch {
            try {
                // 1. Patients
                clientDao.getUnsyncedClients().forEach { client ->
                    clientsRef.child(client.id).setValue(client.copy(isSynced = true)).await()
                    clientDao.markAsSynced(client.id)
                }
                // 2. Lieux
                lieuDao.getUnsynced().forEach { lieu ->
                    lieuxRef.child(lieu.id).setValue(lieu.copy(isSynced = true)).await()
                    lieuDao.markAsSynced(lieu.id)
                }
                // 3. Collegues
                collegueDao.getUnsynced().forEach { col ->
                    colleguesRef.child(col.id).setValue(col.copy(isSynced = true)).await()
                    collegueDao.markAsSynced(col.id)
                }
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Synchronisation manuelle terminée ✅", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showError("Erreur sync manuelle: ${e.message}")
            }
        }
    }

    // --- SYNC ENGINE ---
    private fun scheduleSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync_all", ExistingWorkPolicy.REPLACE, syncRequest)
    }

    fun startFirebaseSync() {
        startClientsSync()
        startLieuxSync()
        startColleguesSync()
    }

    private fun startClientsSync() {
        clientsRef.addChildEventListener(object : ChildEventListener {
            private fun handle(snapshot: DataSnapshot) {
                repositoryScope.launch {
                    try {
                        val remote = snapshot.getValue(ClientEntity::class.java)
                        if (remote == null) {
                            Log.e("AppRepository", "Patient format invalide (null) à l'id: ${snapshot.key}")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Erreur format Patient", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        val local = clientDao.getClientById(remote.id)
                        if (local == null || remote.updatedAt > local.updatedAt) {
                            clientDao.insertOrUpdate(remote.copy(isSynced = true))
                            Log.d("AppRepository", "Patient mis à jour via Firebase: ${remote.nom}")
                            
                            // Feedback visuel
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Patient reçu : ${remote.nom}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Erreur mapping Patient", e)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Donnée Patient illisible", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onChildAdded(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildChanged(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildRemoved(s: DataSnapshot) {
                repositoryScope.launch { s.key?.let { clientDao.softDelete(it, System.currentTimeMillis()) } }
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {
                Log.e("AppRepository", "Firebase Patients Sync Cancelled: ${e.message} - ${e.details}")
            }
        })
    }

    private fun startLieuxSync() {
        lieuxRef.addChildEventListener(object : ChildEventListener {
            private fun handle(snapshot: DataSnapshot) {
                repositoryScope.launch {
                    try {
                        val remote = snapshot.getValue(LieuEntity::class.java) ?: return@launch
                        val local = lieuDao.getLieuById(remote.id)
                        if (local == null || remote.lastModified > local.lastModified) {
                            lieuDao.insertOrUpdate(remote.copy(isSynced = true))
                            Log.d("AppRepository", "Lieu mis à jour via Firebase: ${remote.nomLieu}")
                            
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Code Lieu reçu : ${remote.nomLieu}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Erreur mapping Lieu", e)
                    }
                }
            }
            override fun onChildAdded(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildChanged(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildRemoved(s: DataSnapshot) {
                repositoryScope.launch { s.key?.let { lieuDao.deleteById(it) } }
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {
                Log.e("AppRepository", "Firebase Lieux Sync Cancelled: ${e.message} - ${e.details}")
            }
        })
    }

    private fun startColleguesSync() {
        colleguesRef.addChildEventListener(object : ChildEventListener {
            private fun handle(snapshot: DataSnapshot) {
                repositoryScope.launch {
                    try {
                        val remote = snapshot.getValue(CollegueEntity::class.java) ?: return@launch
                        val local = collegueDao.getCollegueById(remote.id)
                        if (local == null || remote.lastModified > local.lastModified) {
                            collegueDao.insertOrUpdate(remote.copy(isSynced = true))
                            Log.d("AppRepository", "Collegue mis à jour via Firebase: ${remote.prenom}")
                            
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Collègue connecté : ${remote.prenom}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Erreur mapping Collegue", e)
                    }
                }
            }
            override fun onChildAdded(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildChanged(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildRemoved(s: DataSnapshot) {
                repositoryScope.launch { s.key?.let { collegueDao.deleteById(it) } }
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {
                Log.e("AppRepository", "Firebase Collegues Sync Cancelled: ${e.message} - ${e.details}")
            }
        })
    }
}
