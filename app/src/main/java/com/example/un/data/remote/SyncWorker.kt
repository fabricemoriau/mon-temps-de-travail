package com.example.un.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.un.data.local.AppDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val clientDao = database.clientDao()
        val firebaseRef = FirebaseDatabase.getInstance().getReference("shared").child("clients")

        return try {
            val unsynced = clientDao.getUnsyncedClients()
            Log.d("SyncWorker", "Début synchro de ${unsynced.size} fiches")

            unsynced.forEach { client ->
                // Envoi à Firebase
                firebaseRef.child(client.id).setValue(client).await()
                // Si réussi, marquer comme synchronisé
                clientDao.markAsSynced(client.id)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erreur synchro, réessai planifié", e)
            Result.retry()
        }
    }
}
