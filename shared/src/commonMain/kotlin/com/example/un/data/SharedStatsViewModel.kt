package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.WorkDayEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class SharedStatsViewModel(private val coroutineScope: CoroutineScope) {
    private val workDayDao by lazy { 
        try { DatabaseHolder.get().workDayDao() } catch(e: Exception) { null }
    }

    private val _stats = MutableStateFlow<MonthStats?>(null)
    val stats: StateFlow<MonthStats?> = _stats

    fun loadMonthStats(year: Int, month: Month) {
        coroutineScope.launch(Dispatchers.IO) {
            val dao = workDayDao ?: return@launch
            
            val start = LocalDateTime(year, month, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            
            val nextMonthVal = if (month.number == 12) 1 else month.number + 1
            val nextYearVal = if (month.number == 12) year + 1 else year
            val end = LocalDateTime(nextYearVal, Month(nextMonthVal), 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() - 1

            val days = dao.getWorkDaysInRange(start, end)
            
            var totalEff = 0L
            var totalAmp = 0L
            var totalNight = 0L
            var totalSup = 0L
            var workedCount = 0
            var offCount = 0

            days.forEach { wd ->
                totalEff += wd.effectiveMillis
                totalAmp += wd.amplitudeMillis
                totalNight += wd.nightMillis
                totalSup += wd.supMillis
                if (wd.effectiveMillis > 0 && !wd.isRTT && !wd.isVacation) workedCount++
                else if (wd.isRTT || wd.isVacation) offCount++
            }

            _stats.value = MonthStats(
                totalEffective = totalEff,
                totalAmplitude = totalAmp,
                totalNight = totalNight,
                totalSup = totalSup,
                workedDaysCount = workedCount,
                offDaysCount = offCount,
                dailyData = days
            )
        }
    }

    data class MonthStats(
        val totalEffective: Long,
        val totalAmplitude: Long,
        val totalNight: Long,
        val totalSup: Long,
        val workedDaysCount: Int,
        val offDaysCount: Int,
        val dailyData: List<WorkDayEntity>
    )
}
