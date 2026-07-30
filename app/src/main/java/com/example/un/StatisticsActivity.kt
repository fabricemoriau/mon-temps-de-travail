package com.example.un

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.WorkDayEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private var currentMonth = Calendar.getInstance()
    private val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Statistiques"

        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            loadStats()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            loadStats()
        }

        loadStats()
    }

    private fun loadStats() {
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

        findViewById<TextView>(R.id.tvCurrentMonthYear).text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            val days = AppDatabase.getDatabase(this@StatisticsActivity).workDayDao()
                .getWorkDaysInRange(startMonth.timeInMillis, endMonth.timeInMillis)

            var totalMillis = 0L
            var totalPauseMillis = 0L
            var totalAmplitudeMillis = 0L
            var workedDaysCount = 0
            var offDaysCount = 0
            
            days.forEach { wd ->
                totalMillis += wd.effectiveMillis
                totalAmplitudeMillis += wd.amplitudeMillis
                
                // Calcul des pauses
                val pauses = wd.amplitudeMillis - wd.effectiveMillis
                if (pauses > 0) totalPauseMillis += pauses

                if (wd.effectiveMillis > 0 && !wd.isVacation && !wd.isRTT) {
                    workedDaysCount++
                } else if (wd.isVacation || wd.isRTT) {
                    offDaysCount++
                }
            }

            val h = totalMillis / (1000 * 3600)
            val m = (totalMillis / (1000 * 60)) % 60
            
            findViewById<TextView>(R.id.tvStatsDetails).text = String.format(Locale.FRANCE, 
                "Mois : %d jours travaillés / %d repos\nHeures Effectives : %02dh%02d\nAmplitude Totale : %s", 
                workedDaysCount, offDaysCount, h, m, formatMillis(totalAmplitudeMillis)
            )

            // Analyse Pauses
            val hP = totalPauseMillis / (1000 * 3600)
            val mP = (totalPauseMillis / (1000 * 60)) % 60
            findViewById<TextView>(R.id.tvPauseAnalysis).text = String.format(Locale.FRANCE, "Total Pauses : %02dh%02d", hP, mP)
            
            val pb = findViewById<ProgressBar>(R.id.pbWorkPauseRatio)
            if (totalAmplitudeMillis > 0) {
                val ratio = (totalMillis * 100 / totalAmplitudeMillis).toInt()
                pb.progress = ratio
            }

            setupChart(days)
        }
    }

    private fun formatMillis(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format(Locale.FRANCE, "%02dh%02d", h, m)
    }

    private fun setupChart(history: List<com.example.un.data.local.WorkDayEntity>) {
        val chart = findViewById<LineChart>(R.id.lineChart)
        val entries = mutableListOf<Entry>()
        
        history.takeLast(10).forEachIndexed { index, workDay ->
            val hours = workDay.effectiveMillis / (1000f * 3600f)
            entries.add(Entry(index.toFloat(), hours))
        }

        val dataSet = LineDataSet(entries, "Heures Effectives (Dernières saisies)")
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
