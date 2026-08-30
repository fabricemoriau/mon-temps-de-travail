package com.example.un

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.AdminConfig
import com.example.un.utils.AppUpdateManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.un.data.LocalDataManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppUpdateManager.checkUpdates(this)

        checkIfBlocked()

        findViewById<Button>(R.id.btnGoToClients).setOnClickListener {
            startActivity(Intent(this, ClientsActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenGoogleCalendar).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible d'ouvrir l'agenda", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnGoToSharedMessaging).setOnClickListener {
            startActivity(Intent(this, ForumActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToAgenda).setOnClickListener {
            startActivity(Intent(this, AgendaActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToWorkHistory).setOnClickListener {
            startActivity(Intent(this, WorkHistoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToSalary).setOnClickListener {
            startActivity(Intent(this, SalaryActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToScanner).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToStats).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToLieux).setOnClickListener {
            startActivity(Intent(this, LieuxActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToDocs).setOnClickListener {
            startActivity(Intent(this, DocsActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToCollegues).setOnClickListener {
            startActivity(Intent(this, ColleguesActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToCompose).setOnClickListener {
            startActivity(Intent(this, ComposeActivity::class.java))
        }

        findViewById<Button>(R.id.btnCheckUpdate).setOnClickListener {
            AppUpdateManager.checkUpdates(this, manualCheck = true)
        }

        val btnPublish = findViewById<Button>(R.id.btnPublishUpdate)
        if (AdminConfig.isMaster(this)) {
            btnPublish.visibility = View.VISIBLE
            btnPublish.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Publication de version")
                    .setMessage("Voulez-vous définir cette version (${packageManager.getPackageInfo(packageName, 0).versionName}) comme la dernière version officielle sur Internet ?")
                    .setPositiveButton("Publier") { _, _ ->
                        AppUpdateManager.publishCurrentVersion(this)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }

        monitorConnection()
        checkYesterdayEntry()
        performDiagnostic()
    }

    private fun performDiagnostic() {
        val tvStatus = findViewById<TextView>(R.id.tvConnectionStatus)
        val database = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
        val rootRef = database.reference
        
        // 1. Test d'écriture simple
        rootRef.child("shared/diagnostic").setValue(mapOf("time" to System.currentTimeMillis())).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 2. Scan de la structure si l'écriture marche
                rootRef.get().addOnSuccessListener { snapshot ->
                    val dossiers = snapshot.children.mapNotNull { it.key }.joinToString(", ")
                    val patientCount = snapshot.child("shared/clients").childrenCount
                    val msg = "✅ OK | Patients: $patientCount | Dossiers: $dossiers"
                    tvStatus.text = msg
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                    Log.d("Diagnostic", msg)
                }.addOnFailureListener {
                    tvStatus.text = "❌ Erreur Lecture : ${it.message}"
                }
            } else {
                val error = task.exception?.message ?: "Erreur inconnue"
                tvStatus.text = "❌ Erreur Partage : $error"
                tvStatus.setTextColor(Color.RED)
                if (error.contains("Permission denied", ignoreCase = true)) {
                    showFirebaseSetupAlert()
                }
            }
        }
    }

    private fun showFirebaseSetupAlert() {
        AlertDialog.Builder(this)
            .setTitle("Paramétrage Firebase Incomplet")
            .setMessage("L'application n'a pas l'autorisation d'écrire sur Firebase.\n\n" +
                    "Vérifiez l'onglet 'Règles' dans votre console Firebase Realtime Database.\n" +
                    "Les règles doivent autoriser '.read' et '.write' à 'true'.")
            .setPositiveButton("J'ai compris", null)
            .show()
    }

    private fun monitorConnection() {
        val tvStatus = findViewById<TextView>(R.id.tvConnectionStatus)
        val connectedRef = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    tvStatus.text = getString(R.string.msg_connected)
                    tvStatus.setTextColor(Color.GREEN)
                } else {
                    tvStatus.text = getString(R.string.msg_not_connected)
                    tvStatus.setTextColor(Color.RED)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkIfBlocked() {
        // Un utilisateur Maître ne peut pas être bloqué localement (sécurité)
        if (AdminConfig.isMaster(this)) return

        val userId = LocalDataManager.getUserId(this)
        val blockedRef = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference(AdminConfig.PATH_BLOCKED_USERS).child(userId)
        
        blockedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == true) {
                    showBlockedDialog()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showBlockedDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Accès Refusé")
            .setMessage("Votre accès à cette application a été révoqué par l'administrateur.")
            .setCancelable(false)
            .setPositiveButton("Fermer") { _, _ -> finish() }
            .show()
    }

    private fun checkYesterdayEntry() {
        val prefs = getSharedPreferences("SecurityPrefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())
        
        // Si la bannière a été fermée aujourd'hui, on ne l'affiche plus
        if (prefs.getBoolean("banner_closed_$today", false)) return

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayId = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(calendar.time)

        lifecycleScope.launch {
            val entry = AppDatabase.getDatabase(this@MainActivity).workDayDao().getWorkDayById(yesterdayId)
            if (entry == null) {
                showSecurityBanner()
            }
        }
    }

    private fun showSecurityBanner() {
        val card = findViewById<View>(R.id.cardYesterdayAlert)
        val btnSaisir = findViewById<Button>(R.id.btnFixYesterday)
        val btnClose = findViewById<ImageButton>(R.id.btnCloseAlert)
        
        card.visibility = View.VISIBLE
        
        btnSaisir.setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            intent.putExtra("SELECTED_DATE_MILLIS", calendar.timeInMillis)
            startActivity(intent)
        }
        
        btnClose.setOnClickListener {
            card.visibility = View.GONE
            // On mémorise la fermeture pour la journée
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())
            getSharedPreferences("SecurityPrefs", MODE_PRIVATE).edit().putBoolean("banner_closed_$today", true).apply()
        }
    }
}
