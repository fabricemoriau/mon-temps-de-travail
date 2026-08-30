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
import com.example.un.data.local.DatabaseHelper
import com.example.un.utils.HolidayHelper
import com.example.un.utils.toLocalDate
import com.example.un.utils.PdfGenerator
import com.example.un.utils.SalaryCalculator
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
        val cbIsAmbulancier = findViewById<CheckBox>(R.id.cbIsAmbulancier)
        val cbIsTaxi = findViewById<CheckBox>(R.id.cbIsTaxi)

        // Charger les préférences
        val sharedPref = getSharedPreferences("SalaryPrefs", MODE_PRIVATE)
        val profilePrefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        
        etTaux.setText(sharedPref.getString("taux_horaire", profilePrefs.getString("taux_net_base", "12.50")))
        etTauxPanier.setText(sharedPref.getString("taux_panier", profilePrefs.getString("taux_panier_prof", "9.20")))
        cbTachesComp.isChecked = sharedPref.getBoolean("taches_comp", true)
        cbIsAmbulancier.isChecked = sharedPref.getBoolean("is_ambu", true)
        cbIsTaxi.isChecked = sharedPref.getBoolean("is_taxi", false)
        
        // Auto-activation des 5% si >= 2 qualifications dans le profil
        val userPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val qualifCount = listOf(
            userPrefs.getBoolean("qualif_dea", false),
            userPrefs.getBoolean("qualif_taxi", false),
            userPrefs.getBoolean("qualif_aux", false),
            userPrefs.getBoolean("qualif_autre", false)
        ).count { it }
        
        if (qualifCount >= 2) {
            cbTachesComp.isChecked = true
        }
        
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
            override fun afterTextChanged(s: Editable?) { 
                saveRates()
                calculateSalary() 
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etTaux.addTextChangedListener(watcher)
        etTauxPanier.addTextChangedListener(watcher)
        cbTachesComp.setOnCheckedChangeListener { _, _ -> 
            saveRates()
            calculateSalary() 
        }
        cbIsAmbulancier.setOnCheckedChangeListener { _, _ ->
            saveRates()
            calculateSalary()
        }
        cbIsTaxi.setOnCheckedChangeListener { _, _ ->
            saveRates()
            calculateSalary()
        }

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
            putBoolean("is_ambu", findViewById<CheckBox>(R.id.cbIsAmbulancier).isChecked)
            putBoolean("is_taxi", findViewById<CheckBox>(R.id.cbIsTaxi).isChecked)
            apply()
        }
    }

    private fun calculateSalary() {
        val taux = findViewById<EditText>(R.id.etTauxHoraire).text.toString().toDoubleOrNull() ?: 12.5
        val prixPanier = findViewById<EditText>(R.id.etTauxPanier).text.toString().toDoubleOrNull() ?: 8.5
        val has5Percent = findViewById<CheckBox>(R.id.cbTachesComp).isChecked
        val isAmbu = findViewById<CheckBox>(R.id.cbIsAmbulancier).isChecked
        val isTaxi = findViewById<CheckBox>(R.id.cbIsTaxi).isChecked

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
            val days = DatabaseHelper.getDatabase(this@SalaryActivity).workDayDao()
                .getWorkDaysInRange(startCal.timeInMillis, endCal.timeInMillis)

            var totalEffMillis = 0L
            var totalSupMillis = 0L
            var totalNightMillis = 0L
            var totalSundayHolidayMillis = 0L
            var panierCount = 0
            var gardeCount = 0
            var sundayHolidayCount = 0

            days.forEach { wd ->
                totalEffMillis += wd.effectiveMillis
                totalSupMillis += wd.supMillis
                totalNightMillis += wd.nightMillis
                if (wd.hasExtraRepas) panierCount++
                if (wd.isGardeJour || wd.isGardeNuit) gardeCount++

                // Majoration Dimanche / Férié
                val cal = Calendar.getInstance().apply { timeInMillis = wd.timestamp }
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || HolidayHelper.isHoliday(cal.toLocalDate())) {
                    totalSundayHolidayMillis += wd.effectiveMillis
                    sundayHolidayCount++
                }
            }
            
            val profilePrefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
            val customPrimeGarde = profilePrefs.getString("prime_garde_samu", "30.00")?.toDoubleOrNull() ?: 30.0
            val customBaseHeures = profilePrefs.getString("base_heures", "151.67")?.toDoubleOrNull() ?: 151.67
            val customMajorNuit = profilePrefs.getString("major_nuit", "25.0")?.toDoubleOrNull() ?: 25.0
            val customPrimeDimanche = profilePrefs.getString("prime_dimanche", "26.30")?.toDoubleOrNull() ?: 26.30
            val customIsCalcMensuel = profilePrefs.getBoolean("calc_mensuel", false)

            val result = SalaryCalculator.calculate(
                isAmbulancier = isAmbu,
                isTaxi = isTaxi,
                totalEffMillis = totalEffMillis,
                totalSupMillis = totalSupMillis,
                totalNightMillis = totalNightMillis,
                sundayHolidayEffMillis = totalSundayHolidayMillis,
                tauxHoraire = taux,
                panierCount = panierCount,
                tauxPanier = prixPanier,
                gardeCount = gardeCount,
                has5PercentComp = has5Percent,
                sundayHolidayCount = sundayHolidayCount,
                primeGardeSamu = customPrimeGarde,
                baseHeures = customBaseHeures,
                tauxMajorNuit = customMajorNuit,
                primeDimanche = customPrimeDimanche,
                isCalcMensuel = customIsCalcMensuel
            )

            findViewById<TextView>(R.id.tvResultBrut).text = String.format(Locale.FRANCE, "Total Net : %.2f €", result.totalNet)
            findViewById<TextView>(R.id.tvResultDetails).text = result.details
        }
    }

    private fun exportSalaryPdf() {
        val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val name = sharedPref.getString("nom", "") ?: ""
        val prenom = sharedPref.getString("prenom", "") ?: ""
        val fullName = "$prenom $name".trim().ifEmpty { "Utilisateur non renseigné" }
        
        val monthName = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }
        val details = findViewById<TextView>(R.id.tvResultDetails).text.toString()
        val totalNet = findViewById<TextView>(R.id.tvResultBrut).text.toString()
        
        val file = PdfGenerator.generateSalaryReport(this, monthName, details, totalNet, fullName)
        
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
