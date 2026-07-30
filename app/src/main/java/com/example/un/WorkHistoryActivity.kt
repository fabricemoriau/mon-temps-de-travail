package com.example.un

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.WorkDayEntity
import com.example.un.utils.CalendarImporter
import com.example.un.utils.PdfGenerator
import com.example.un.utils.ShareUtils
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WorkHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: WorkHistoryAdapter
    private lateinit var gridAdapter: CalendarGridAdapter
    private lateinit var tabLayout: TabLayout
    private var currentMonth = Calendar.getInstance()
    private val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
    private val sdfTab = SimpleDateFormat("MMM yy", Locale.FRANCE)
    private val sdfId = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    
    private val tabMonths = mutableListOf<Calendar>()
    
    private var lastLoadedDays = listOf<WorkDayEntity>()
    private var lastTotalMillis = 0L

    companion object {
        private const val PERM_CALENDAR = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agenda Mensuel"

        val rv = findViewById<RecyclerView>(R.id.rvWorkHistory)
        adapter = WorkHistoryAdapter { wd: WorkDayEntity ->
            val intent = Intent(this, AgendaActivity::class.java)
            intent.putExtra("SELECTED_DATE_MILLIS", wd.timestamp)
            startActivity(intent)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val rvGrid = findViewById<RecyclerView>(R.id.rvCalendarGrid)
        gridAdapter = CalendarGridAdapter { wd ->
            if (wd.dateId.startsWith("pad_")) return@CalendarGridAdapter
            
            // Clic sur un jour de la grille : scroller la liste ou ouvrir l'agenda
            if (wd.vehiculeType != "Non saisi" || wd.isVacation || wd.isRTT) {
                val index = adapter.currentList.indexOfFirst { it.dateId == wd.dateId }
                if (index != -1) rv.scrollToPosition(index)
            } else {
                val intent = Intent(this, AgendaActivity::class.java)
                intent.putExtra("SELECTED_DATE_MILLIS", wd.timestamp)
                startActivity(intent)
            }
        }
        rvGrid.layoutManager = GridLayoutManager(this, 7)
        rvGrid.adapter = gridAdapter

        tabLayout = findViewById(R.id.tabMonthSelector)
        setupTabs()

        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            refreshHistory()
        }

        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            refreshHistory()
        }

        findViewById<ImageButton>(R.id.btnSearchMonth).setOnClickListener {
            showMonthPicker()
        }

        findViewById<Button>(R.id.btnSetVacation).setOnClickListener {
            startActivity(Intent(this, VacationPeriodActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnShareMonth).setOnClickListener {
            showExportOptions()
        }

        findViewById<Button>(R.id.btnSyncGoogle).setOnClickListener {
            checkCalendarPermission()
        }

        refreshHistory()
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()
        tabMonths.clear()

        // Générer les 12 derniers mois (du plus récent au plus ancien)
        val cal = Calendar.getInstance()
        for (i in 0 until 12) {
            val monthCal = cal.clone() as Calendar
            tabMonths.add(monthCal)
            
            val tab = tabLayout.newTab()
            tab.text = sdfTab.format(monthCal.time).replaceFirstChar { it.uppercase() }
            tabLayout.addTab(tab)
            
            cal.add(Calendar.MONTH, -1)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val index = tab?.position ?: 0
                if (index < tabMonths.size) {
                    currentMonth.timeInMillis = tabMonths[index].timeInMillis
                    refreshHistory(updateTabs = false)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateSelectedTab() {
        val currentMonthVal = currentMonth.get(Calendar.MONTH)
        val currentYearVal = currentMonth.get(Calendar.YEAR)
        
        for (i in 0 until tabMonths.size) {
            val m = tabMonths[i]
            if (m.get(Calendar.MONTH) == currentMonthVal && m.get(Calendar.YEAR) == currentYearVal) {
                tabLayout.getTabAt(i)?.select()
                break
            }
        }
    }

    private fun checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), PERM_CALENDAR)
        } else {
            startCalendarSync()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_CALENDAR && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCalendarSync()
        }
    }

    private fun startCalendarSync() {
        val startMonth = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val endMonth = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }

        lifecycleScope.launch {
            val imported = CalendarImporter.importShifts(this@WorkHistoryActivity, startMonth.timeInMillis, endMonth.timeInMillis)
            if (imported.isNotEmpty()) {
                val db = AppDatabase.getDatabase(this@WorkHistoryActivity).workDayDao()
                imported.forEach { db.insert(it) }
                Toast.makeText(this@WorkHistoryActivity, "${imported.size} gardes importées !", Toast.LENGTH_SHORT).show()
                refreshHistory()
            } else {
                Toast.makeText(this@WorkHistoryActivity, "Aucune garde trouvée dans l'agenda Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExportOptions() {
        val options = arrayOf("Partager texte (WhatsApp/Email)", "Générer Rapport PDF Officiel")
        AlertDialog.Builder(this)
            .setTitle("Options d'exportation")
            .setItems(options) { _, which ->
                when (options[which]) {
                    options[0] -> shareMonth()
                    options[1] -> exportPdf()
                }
            }
            .show()
    }

    private fun exportPdf() {
        val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val name = sharedPref.getString("nom", "") ?: ""
        val prenom = sharedPref.getString("prenom", "") ?: ""
        val fullName = "$prenom $name".trim().ifEmpty { "Utilisateur non renseigné" }
        
        val monthName = sdfMonth.format(currentMonth.time)
        val file = PdfGenerator.generateMonthlyReport(this, monthName, lastLoadedDays, fullName)
        
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
        } else {
            Toast.makeText(this, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMonthPicker() {
        DatePickerDialog(this, { _, y, m, _ ->
            currentMonth.set(Calendar.YEAR, y)
            currentMonth.set(Calendar.MONTH, m)
            refreshHistory()
        }, currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH), 1).show()
    }

    private fun shareMonth() {
        val monthName = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }
        val text = ShareUtils.generateMonthSummary(monthName, lastLoadedDays, lastTotalMillis)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Partager le mois"))
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory(updateTabs: Boolean = true) {
        if (updateTabs) updateSelectedTab()
        
        val startMonth = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val endMonth = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        findViewById<TextView>(R.id.tvCurrentMonth).text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            val dbDays = AppDatabase.getDatabase(this@WorkHistoryActivity).workDayDao()
                .getWorkDaysInRange(startMonth.timeInMillis, endMonth.timeInMillis)
            
            lastLoadedDays = dbDays
            
            val dbMap = dbDays.associateBy { it.dateId }
            
            // Générer tous les jours du mois
            val allDaysOfMonth = mutableListOf<WorkDayEntity>()
            val genCal = startMonth.clone() as Calendar
            val maxDay = genCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            for (i in 1..maxDay) {
                val dateId = sdfId.format(genCal.time)
                val existing = dbMap[dateId]
                if (existing != null) {
                    allDaysOfMonth.add(existing)
                } else {
                    // Jour vide par défaut
                    allDaysOfMonth.add(WorkDayEntity(
                        dateId = dateId,
                        timestamp = genCal.timeInMillis,
                        vehiculeType = "Non saisi",
                        vehiculeNum = ""
                    ))
                }
                genCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            adapter.submitList(allDaysOfMonth)

            // Préparer la grille (avec décalage pour le premier jour du mois)
            val gridDays = mutableListOf<WorkDayEntity>()
            val firstDayCal = startMonth.clone() as Calendar
            // En France, Lundi = 2, Mardi = 3 ... Dimanche = 1
            val dayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1=Dim, 2=Lun...
            val offset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            
            for (i in 0 until offset) {
                gridDays.add(WorkDayEntity(dateId = "pad_$i", timestamp = 0, vehiculeType = "Non saisi"))
            }
            gridDays.addAll(allDaysOfMonth)
            gridAdapter.submitList(gridDays)

            var totalMillis = 0L
            dbDays.forEach { totalMillis += it.effectiveMillis }
            lastTotalMillis = totalMillis
            
            val h = totalMillis / (1000 * 3600)
            val m = (totalMillis / (1000 * 60)) % 60
            findViewById<TextView>(R.id.tvMonthTotal).text = String.format(Locale.FRANCE, "Total : %02dh%02d", h, m)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
