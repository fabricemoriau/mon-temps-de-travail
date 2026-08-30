package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.WorkDayEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*

class AgendaViewModel(private val coroutineScope: CoroutineScope) {
    private val workDayDao = DatabaseHolder.get().workDayDao()

    private val _workDay = MutableStateFlow<WorkDayEntity?>(null)
    val workDay: StateFlow<WorkDayEntity?> = _workDay

    fun loadDay(dateId: String, timestamp: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val entry = workDayDao.getWorkDayById(dateId)
            if (entry != null) {
                _workDay.value = entry
            } else {
                _workDay.value = WorkDayEntity(
                    dateId = dateId,
                    timestamp = timestamp
                )
            }
        }
    }

    fun saveDay(entry: WorkDayEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            workDayDao.insert(entry)
            _workDay.value = entry
        }
    }

    fun calculateDailyStats(entry: WorkDayEntity): DailyStats {
        if (entry.isRTT) return DailyStats(0, 0, 0, 0)
        
        var amplitude = entry.endMillis - entry.startMillis
        if (amplitude < 0) amplitude += 24 * 3600 * 1000

        if (entry.isGardeJour || entry.isGardeNuit) {
            amplitude = 12 * 3600 * 1000
        }

        val effective = amplitude
        
        val sup = if (entry.isAllSup) {
            amplitude
        } else {
            val base = (8.5 * 3600 * 1000).toLong()
            if (amplitude > base) {
                val diffMin = (amplitude - base) / 60000
                (diffMin / 15 * 15) * 60000
            } else 0L
        }

        val night = calculateNightMillis(entry.startMillis, entry.endMillis)

        return DailyStats(amplitude, effective, sup, night)
    }

    private fun calculateNightMillis(start: Long, end: Long): Long {
        if (start == 0L || end == 0L) return 0L
        var nightDuration = 0L
        
        val instantStart = Instant.fromEpochMilliseconds(start)
        val instantEnd = if (end > start) {
            Instant.fromEpochMilliseconds(end)
        } else {
            Instant.fromEpochMilliseconds(end + 24 * 3600 * 1000)
        }
        
        var current = instantStart
        val timeZone = TimeZone.currentSystemDefault()
        
        while (current < instantEnd) {
            val hour = current.toLocalDateTime(timeZone).hour
            if (hour >= 21 || hour < 6) {
                nightDuration += 60 * 1000
            }
            current = current.plus(1, DateTimeUnit.MINUTE)
        }
        return nightDuration
    }

    data class DailyStats(
        val amplitude: Long,
        val effective: Long,
        val sup: Long,
        val night: Long
    )
}
