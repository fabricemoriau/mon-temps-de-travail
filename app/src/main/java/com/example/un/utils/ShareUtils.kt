package com.example.un.utils

import com.example.un.data.local.WorkDayEntity
import java.text.SimpleDateFormat
import java.util.*

object ShareUtils {

    private val sdfDisplay = SimpleDateFormat("EEE d MMM", Locale.FRANCE)

    fun generateDaySummary(wd: WorkDayEntity): String {
        val sb = StringBuilder()
        val dateStr = sdfDisplay.format(Date(wd.timestamp))
        sb.append("📋 Récapitulatif du $dateStr\n")
        sb.append("----------------------------\n")

        if (wd.isVacation) {
            sb.append("🏖️ VACANCES / CONGÉS\n")
        } else if (wd.isRTT) {
            sb.append("🧘 REPOS / RTT\n")
        } else {
            if (wd.isGardeJour || wd.isGardeNuit) {
                val type = if (wd.isGardeJour) "Jour" else "Nuit"
                sb.append("🚑 GARDE SAMU ($type)\n")
            }
            if (wd.vehiculeType.isNotEmpty()) {
                sb.append("🚗 Véhicule : ${wd.vehiculeType} ${wd.vehiculeNum}\n")
            }
            sb.append("🕒 Début : ${formatTime(wd.startMillis)}\n")
            sb.append("🕒 Fin : ${formatTime(wd.endMillis)}\n")
            
            if (wd.effectiveMillis > 0) {
                sb.append("✅ Temps effectif : ${formatDuration(wd.effectiveMillis)}\n")
            }
            if (wd.supMillis > 0) {
                sb.append("➕ Heures supp : ${formatDuration(wd.supMillis)}\n")
            }
        }
        return sb.toString()
    }

    fun generateMonthSummary(monthName: String, days: List<WorkDayEntity>, totalMillis: Long): String {
        val sb = StringBuilder()
        sb.append("📅 BILAN MENSUEL : $monthName\n")
        sb.append("============================\n\n")

        val workedDays = days.filter { it.effectiveMillis > 0 || it.isGardeJour || it.isGardeNuit }
        
        workedDays.forEach { wd ->
            val dateStr = sdfDisplay.format(Date(wd.timestamp))
            val duration = formatDuration(wd.effectiveMillis)
            val info = when {
                wd.isGardeJour -> " (Garde Jour)"
                wd.isGardeNuit -> " (Garde Nuit)"
                else -> ""
            }
            sb.append("• $dateStr : $duration$info\n")
        }

        sb.append("\n----------------------------\n")
        sb.append("📊 TOTAL MOIS : ${formatDuration(totalMillis)}\n")
        sb.append("🏢 Jours travaillés : ${workedDays.size}\n")
        
        val vacationCount = days.count { it.isVacation }
        if (vacationCount > 0) sb.append("🏖️ Vacances : $vacationCount jours\n")
        
        return sb.toString()
    }

    private fun formatTime(millis: Long): String {
        if (millis <= 0) return "--:--"
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun formatDuration(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format("%02dh%02d", h, m)
    }
}
