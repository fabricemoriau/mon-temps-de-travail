package com.example.un

import android.app.Application
import com.example.un.data.ClientRepository
import com.example.un.data.local.AppDatabase
import com.google.firebase.database.FirebaseDatabase

class MonTempsApp : Application() {

    // On utilise lazy pour ne pas ralentir le démarrage
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        ClientRepository(
            database.clientDao(), 
            FirebaseDatabase.getInstance().getReference("shared").child("clients"),
            this
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // Activation de la persistance Firebase pour le mode hors-ligne
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Déjà activé ou erreur de config
        }
        // Lancement de l'écouteur de synchronisation miroir
        repository.startFirebaseSync()
    }
}
