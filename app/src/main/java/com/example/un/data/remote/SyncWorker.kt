package com.example.un.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.un.data.AdminConfig
import com.example.un.data.local.DatabaseHelper
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = DatabaseHelper.getDatabase(applicationContext)
        val firebase = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
        
        val clientDao = database.clientDao()
        val lieuDao = database.lieuDao()
        val collegueDao = database.collegueDao()

        return try {
            // 1. Sync Patients
            val unsyncedClients = clientDao.getUnsyncedClients()
            Log.d("SyncWorker", "Synchro de ${unsyncedClients.size} patients")
            unsyncedClients.forEach { client ->
                firebase.getReference(AdminConfig.PATH_SHARED_PATIENTS).child(client.id)
                    .setValue(client.copy(isSynced = true)).await()
                clientDao.markAsSynced(client.id)
            }

            // 2. Sync Lieux
            val unsyncedLieux = lieuDao.getUnsynced()
            Log.d("SyncWorker", "Synchro de ${unsyncedLieux.size} lieux")
            unsyncedLieux.forEach { lieu ->
                firebase.getReference("shared/lieux_codes").child(lieu.id)
                    .setValue(lieu.copy(isSynced = true)).await()
                lieuDao.markAsSynced(lieu.id)
            }

            // 3. Sync Collegues
            val unsyncedCol = collegueDao.getUnsynced()
            Log.d("SyncWorker", "Synchro de ${unsyncedCol.size} collegues")
            unsyncedCol.forEach { col ->
                firebase.getReference(AdminConfig.PATH_SHARED_COLLEGUES).child(col.id)
                    .setValue(col.copy(isSynced = true)).await()
                collegueDao.markAsSynced(col.id)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erreur synchro, réessai planifié", e)
            Result.retry()
        }
    }
}
