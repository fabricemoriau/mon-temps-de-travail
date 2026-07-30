package com.example.un.data

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.un.data.local.ClientDao
import com.example.un.data.local.ClientEntity
import com.example.un.data.remote.SyncWorker
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ClientRepository(
    private val clientDao: ClientDao,
    private val firebaseRef: DatabaseReference,
    private val context: Context
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val allClients: Flow<List<ClientEntity>> = clientDao.getAllVisibleClients()

    suspend fun saveClient(client: ClientEntity) {
        clientDao.insertOrUpdate(client.copy(isSynced = false))
        scheduleSync()
    }

    suspend fun softDeleteClient(id: String) {
        clientDao.softDelete(id, System.currentTimeMillis())
        scheduleSync()
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_clients",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun startFirebaseSync() {
        try {
            firebaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    repositoryScope.launch {
                        try {
                            snapshot.children.forEach { child ->
                                val remote = child.getValue(ClientEntity::class.java)
                                if (remote != null) {
                                    val local = clientDao.getClientById(remote.id)
                                    if (local == null || remote.updatedAt > local.updatedAt) {
                                        clientDao.insertOrUpdate(remote.copy(isSynced = true))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Repository", "Error merging Firebase data", e)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("Repository", "Firebase sync cancelled", error.toException())
                }
            })
        } catch (e: Exception) {
            Log.e("Repository", "Firebase initialization failed", e)
        }
    }
}
