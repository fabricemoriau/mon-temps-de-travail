package com.example.un

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.WorkDayEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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

        findViewById<android.view.View>(R.id.btnPrintStats).setOnClickListener {
            exportToCSV()
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

        val prevMonthStart = (startMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthEnd = (endMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }

        findViewById<TextView>(R.id.tvCurrentMonthYear).text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            val days = AppDatabase.getDatabase(this@StatisticsActivity).workDayDao()
                .getWorkDaysInRange(startMonth.timeInMillis, endMonth.timeInMillis)
            
            val prevDays = AppDatabase.getDatabase(this@StatisticsActivity).workDayDao()
                .getWorkDaysInRange(prevMonthStart.timeInMillis, prevMonthEnd.timeInMillis)

            var totalMillis = 0L
            var totalPauseMillis = 0L
            var totalAmplitudeMillis = 0L
            var totalNightMillis = 0L
            var totalSupMillis = 0L
            var workedDaysCount = 0
            var offDaysCount = 0
            
            days.forEach { wd ->
                totalMillis += wd.effectiveMillis
                totalAmplitudeMillis += wd.amplitudeMillis
                totalNightMillis += wd.nightMillis
                totalSupMillis += wd.supMillis
                
                // Calcul des pauses
                val pauses = wd.amplitudeMillis - wd.effectiveMillis
                if (pauses > 0) totalPauseMillis += pauses

                if (wd.effectiveMillis > 0 && !wd.isVacation && !wd.isRTT) {
                    workedDaysCount++
                } else if (wd.isVacation || wd.isRTT) {
                    offDaysCount++
                }
            }

            // Comparaison
            val prevTotalMillis = prevDays.sumOf { it.effectiveMillis }
            val comparisonText = if (prevTotalMillis > 0) {
                val diff = (totalMillis - prevTotalMillis).toFloat() / prevTotalMillis * 100
                val sign = if (diff >= 0) "+" else ""
                String.format(Locale.FRANCE, "Comparaison avec le mois dernier : %s%.1f%%", sign, diff)
            } else {
                "Comparaison avec le mois dernier : --%"
            }
            findViewById<TextView>(R.id.tvComparisonInfo).text = comparisonText

            findViewById<TextView>(R.id.tvNightHours).text = formatMillis(totalNightMillis)
            findViewById<TextView>(R.id.tvSupHours).text = formatMillis(totalSupMillis)

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

            setupChart(days, startMonth)
        }
    }

    private fun formatMillis(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format(Locale.FRANCE, "%02dh%02d", h, m)
    }

    private fun setupChart(history: List<WorkDayEntity>, startMonth: Calendar) {
        val chart = findViewById<LineChart>(R.id.lineChart)
        val entries = mutableListOf<Entry>()
        
        val maxDays = startMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayMap = history.associateBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }

        for (i in 1..maxDays) {
            val wd = dayMap[i]
            val hours = (wd?.effectiveMillis ?: 0L) / (1000f * 3600f)
            entries.add(Entry(i.toFloat(), hours))
        }

        val dataSet = LineDataSet(entries, "Heures Effectives")
        dataSet.color = getColor(R.color.brand_primary)
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = getColor(R.color.brand_primary)
        dataSet.fillAlpha = 50

        chart.data = LineData(dataSet)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        
        chart.invalidate()
    }

    private fun exportToCSV() {
        lifecycleScope.launch {
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
            
            val days = AppDatabase.getDatabase(this@StatisticsActivity).workDayDao()
                .getWorkDaysInRange(startMonth.timeInMillis, endMonth.timeInMillis)

            val fileName = "Export_Stats_${sdfMonth.format(currentMonth.time).replace(" ", "_")}.csv"
            val file = File(cacheDir, fileName)
            
            try {
                FileOutputStream(file).use { out ->
                    out.write("Date;Debut;Fin;Amplitude;Effectif;Nuit;Sup;Repos\n".toByteArray())
                    days.forEach { wd ->
                        val line = String.format("%s;%s;%s;%s;%s;%s;%s;%s\n",
                            wd.dateId,
                            formatTime(wd.startMillis),
                            formatTime(wd.endMillis),
                            formatMillis(wd.amplitudeMillis),
                            formatMillis(wd.effectiveMillis),
                            formatMillis(wd.nightMillis),
                            formatMillis(wd.supMillis),
                            if (wd.isVacation) "Conges" else if (wd.isRTT) "RTT" else ""
                        )
                        out.write(line.toByteArray())
                    }
                }

                val uri = FileProvider.getUriForFile(this@StatisticsActivity, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Export Statistiques - ${sdfMonth.format(currentMonth.time)}")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Partager l'export CSV"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms == 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
