package com.example.un.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.un.data.local.WorkDayEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun generateMonthlyReport(context: Context, monthName: String, days: List<WorkDayEntity>, userName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Header
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("RAPPORT MENSUEL D'ACTIVITÉ", 50f, 50f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Agent : $userName", 50f, 80f, paint)
        canvas.drawText("Période : $monthName", 50f, 100f, paint)

        // Table Header
        var y = 140f
        paint.isFakeBoldText = true
        canvas.drawText("Date", 50f, y, paint)
        canvas.drawText("Véhicule", 150f, y, paint)
        canvas.drawText("Horaires", 250f, y, paint)
        canvas.drawText("Effectif", 450f, y, paint)
        
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y + 5, 550f, y + 5, paint)
        y += 25f

        // Table Content
        paint.isFakeBoldText = false
        val workedDays = days.filter { it.effectiveMillis > 0 || it.isGardeJour || it.isGardeNuit || it.isVacation || it.isRTT }
        
        workedDays.forEach { wd ->
            if (y > 800) { /* Multi-page logic could be added here */ }
            
            canvas.drawText(sdfDisplay.format(Date(wd.timestamp)), 50f, y, paint)
            
            val vehicule = when {
                wd.isVacation -> "VACANCES"
                wd.isRTT -> "REPOS"
                else -> "${wd.vehiculeType} ${wd.vehiculeNum}"
            }
            canvas.drawText(vehicule, 150f, y, paint)
            
            val horaires = if (wd.startMillis > 0) "${formatTime(wd.startMillis)} - ${formatTime(wd.endMillis)}" else "---"
            canvas.drawText(horaires, 250f, y, paint)
            
            val effectif = if (wd.effectiveMillis > 0) formatDuration(wd.effectiveMillis) else "00h00"
            canvas.drawText(effectif, 450f, y, paint)
            
            y += 20f
        }

        // Totaux
        y += 20f
        paint.isFakeBoldText = true
        val totalMillis = workedDays.sumOf { it.effectiveMillis }
        canvas.drawText("TOTAL GÉNÉRAL : ${formatDuration(totalMillis)}", 350f, y, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Rapport_${monthName.replace(" ", "_")}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    fun generateSalaryReport(context: Context, monthName: String, details: String, totalBrut: String, userName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Header
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("BILAN DE PAIE ESTIMATIF", 50f, 50f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Agent : $userName", 50f, 80f, paint)
        canvas.drawText("Mois : $monthName", 50f, 100f, paint)
        
        canvas.drawLine(50f, 115f, 550f, 115f, paint)

        // Content
        var y = 150f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("DÉTAILS DU CALCUL :", 50f, y, paint)
        
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        
        details.split("\n").forEach { line ->
            canvas.drawText(line, 50f, y, paint)
            y += 20f
        }
        
        y += 20f
        canvas.drawLine(50f, y, 550f, y, paint)
        
        y += 40f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#303F9F")
        canvas.drawText(totalBrut, 50f, y, paint)

        paint.textSize = 10f
        paint.color = Color.GRAY
        paint.isFakeBoldText = false
        canvas.drawText("* Ce document est une estimation basée sur vos saisies.", 50f, 800f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Bilan_Paie_${monthName.replace(" ", "_")}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(Locale.FRANCE, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun formatDuration(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format(Locale.FRANCE, "%02dh%02d", h, m)
    }
}
