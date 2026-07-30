package com.example.un

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.AdminConfig
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
            startActivity(Intent(this, SharedMessagingActivity::class.java))
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

        checkYesterdayEntry()
    }

    private fun checkIfBlocked() {
        val userId = LocalDataManager.getUserId(this)
        val blockedRef = FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_BLOCKED_USERS).child(userId)
        
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
