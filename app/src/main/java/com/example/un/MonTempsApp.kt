package com.example.un

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.AppRepository
import com.example.un.data.local.AppDatabase
import com.example.un.utils.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonTempsApp : Application() {

    // On utilise lazy pour ne pas ralentir le démarrage
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        AppRepository(
            database.clientDao(), 
            database.lieuDao(),
            database.collegueDao(),
            FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL),
            this
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MonTempsApp", "Initialisation de l'application...")
        
        NotificationHelper.createNotificationChannel(this)

        val db = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)

        try {
            // Activation de la persistance Firebase pour le mode hors-ligne
            db.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Déjà activé ou erreur de config
        }

        // Surveillance de la connexion Firebase
        val connectedRef = db.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Toast.makeText(this@MonTempsApp, getString(R.string.msg_connected), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MonTempsApp, getString(R.string.msg_not_connected), Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        // Lancement de la synchronisation globale
        repository.startFirebaseSync()
    }
}
