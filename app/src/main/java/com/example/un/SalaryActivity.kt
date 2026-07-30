package com.example.un

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.utils.HolidayHelper
import com.example.un.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalaryActivity : AppCompatActivity() {

    private var currentMonth = Calendar.getInstance()
    private val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salary)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Calcul de Paie"

        val etTaux = findViewById<EditText>(R.id.etTauxHoraire)
        val etTauxPanier = findViewById<EditText>(R.id.etTauxPanier)
        val cbTachesComp = findViewById<CheckBox>(R.id.cbTachesComp)
        
        // Navigation mensuelle
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            calculateSalary()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            calculateSalary()
        }

        // Liens Conventions
        findViewById<Button>(R.id.btnConvAmbu).setOnClickListener {
            openUrl("https://www.legifrance.gouv.fr/conv_coll/id/KALICONT000005635624")
        }
        findViewById<Button>(R.id.btnConvTaxi).setOnClickListener {
            openUrl("https://www.legifrance.gouv.fr/conv_coll/id/KALICONT000044594539")
        }

        // Scan Paie
        findViewById<Button>(R.id.btnScanPayslip).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("SCAN_TYPE", "PAIE")
            startActivity(intent)
        }

        // Export PDF
        findViewById<Button>(R.id.btnExportSalaryPdf).setOnClickListener {
            exportSalaryPdf()
        }

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calculateSalary() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etTaux.addTextChangedListener(watcher)
        etTauxPanier.addTextChangedListener(watcher)
        cbTachesComp.setOnCheckedChangeListener { _, _ -> calculateSalary() }

        updateMonthDisplay()
        calculateSalary()
    }

    private fun updateMonthDisplay() {
        findViewById<TextView>(R.id.tvCurrentMonth).text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir le lien", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRates() {
        val sharedPref = getSharedPreferences("SalaryPrefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("taux_horaire", findViewById<EditText>(R.id.etTauxHoraire).text.toString())
            putString("taux_panier", findViewById<EditText>(R.id.etTauxPanier).text.toString())
            putBoolean("taches_comp", findViewById<CheckBox>(R.id.cbTachesComp).isChecked)
            apply()
        }
    }

    private fun calculateSalary() {
        val taux = findViewById<EditText>(R.id.etTauxHoraire).text.toString().toDoubleOrNull() ?: 12.5
        val prixPanier = findViewById<EditText>(R.id.etTauxPanier).text.toString().toDoubleOrNull() ?: 8.5
        val has5Percent = findViewById<CheckBox>(R.id.cbTachesComp).isChecked

        val startCal = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val endCal = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        lifecycleScope.launch {
            val days = AppDatabase.getDatabase(this@SalaryActivity).workDayDao()
                .getWorkDaysInRange(startCal.timeInMillis, endCal.timeInMillis)

            var totalEffMillis = 0L
            var totalSup25Millis = 0L
            var totalSup50Millis = 0L
            var totalNightMillis = 0L
            var totalSundayHolidayMillis = 0L
            var panierCount = 0

            days.forEach { wd ->
                totalEffMillis += wd.effectiveMillis
                totalNightMillis += wd.nightMillis
                if (wd.hasExtraRepas) panierCount++

                // Majoration Dimanche / Férié
                val cal = Calendar.getInstance().apply { timeInMillis = wd.timestamp }
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || HolidayHelper.isHoliday(cal)) {
                    totalSundayHolidayMillis += wd.effectiveMillis
                }
            }

            // Calcul Heures Supplémentaires (Simplifié pour le mois)
            // Base légale transport : 151.67h / mois (35h hebdo)
            // Majoration 25% de 151.67 à 186.33h (36h à 43h)
            // Majoration 50% au delà de 186.33h
            val totalEffHours = totalEffMillis / (1000.0 * 3600.0)
            if (totalEffHours > 151.67) {
                val excess = totalEffHours - 151.67
                if (excess > 34.66) { // (43-35)*4.33
                    totalSup25Millis = (34.66 * 3600 * 1000).toLong()
                    totalSup50Millis = ((excess - 34.66) * 3600 * 1000).toLong()
                } else {
                    totalSup25Millis = (excess * 3600 * 1000).toLong()
                }
            }

            val sup25Hours = totalSup25Millis / (1000.0 * 3600.0)
            val sup50Hours = totalSup50Millis / (1000.0 * 3600.0)
            val nightHours = totalNightMillis / (1000.0 * 3600.0)
            val sunHolHours = totalSundayHolidayMillis / (1000.0 * 3600.0)

            // Primes de Permanence (Garde 12h = Prime forfaitaire de 15€ possible + heures effectives)
            val gardeCount = days.count { it.isGardeJour || it.isGardeNuit }
            val payGardePrime = gardeCount * 15.0 

            var basePay = totalEffHours * taux
            if (has5Percent) basePay *= 1.05
            
            val paySup25 = sup25Hours * taux * 0.25
            val paySup50 = sup50Hours * taux * 0.50
            val payNight = nightHours * taux * 0.25
            val paySunHol = sunHolHours * taux * 0.50 
            val totalPaniers = panierCount * prixPanier
            
            val totalBrut = basePay + paySup25 + paySup50 + payNight + paySunHol + payGardePrime + totalPaniers

            findViewById<TextView>(R.id.tvResultBrut).text = String.format(Locale.FRANCE, "Total Brut : %.2f €", totalBrut)
            findViewById<TextView>(R.id.tvResultDetails).text = String.format(Locale.FRANCE, 
                "Heures Totales : %.2f h\n----------------------------\n" +
                "Base (+5%% comp.) : %.2f €\n" +
                "Supp 25%% (%.2f h) : %.2f €\n" +
                "Supp 50%% (%.2f h) : %.2f €\n" +
                "Nuit (+25%%) : %.2f €\n" +
                "Dim/Férié (+50%%) : %.2f €\n" +
                "Primes Gardes (%d) : %.2f €\n" +
                "Paniers (%d) : %.2f €",
                totalEffHours, basePay, sup25Hours, paySup25, sup50Hours, paySup50, payNight, paySunHol, gardeCount, payGardePrime, panierCount, totalPaniers
            )
        }
    }

    private fun exportSalaryPdf() {
        val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val name = sharedPref.getString("nom", "") ?: ""
        val prenom = sharedPref.getString("prenom", "") ?: ""
        val fullName = "$prenom $name".trim().ifEmpty { "Utilisateur non renseigné" }
        
        val monthName = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }
        val details = findViewById<TextView>(R.id.tvResultDetails).text.toString()
        val totalBrut = findViewById<TextView>(R.id.tvResultBrut).text.toString()
        
        val file = PdfGenerator.generateSalaryReport(this, monthName, details, totalBrut, fullName)
        
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Partager le bilan de paie"))
        } else {
            Toast.makeText(this, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
