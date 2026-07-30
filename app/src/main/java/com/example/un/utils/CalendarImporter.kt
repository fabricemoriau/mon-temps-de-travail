package com.example.un.utils

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.un.data.local.WorkDayEntity
import java.text.SimpleDateFormat
import java.util.*

object CalendarImporter {

    private val sdfId = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    fun importShifts(context: Context, startTime: Long, endTime: Long): List<WorkDayEntity> {
        val importedDays = mutableListOf<WorkDayEntity>()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(0) ?: ""
                    val start = it.getLong(1)
                    val end = it.getLong(2)
                    
                    if (isGardeEvent(title)) {
                        val cal = Calendar.getInstance().apply { timeInMillis = start }
                        val dateId = sdfId.format(cal.time)
                        
                        val isNuit = title.contains("nuit", ignoreCase = true) || 
                                     Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.HOUR_OF_DAY) >= 18

                        val wd = WorkDayEntity(
                            dateId = dateId,
                            timestamp = start,
                            startMillis = start,
                            endMillis = end,
                            effectiveMillis = 12 * 60 * 60 * 1000L, // Garde 12h par défaut
                            isGardeJour = !isNuit,
                            isGardeNuit = isNuit,
                            vehiculeType = if (title.contains("SAMU", ignoreCase = true)) "SAMU" else "Garde"
                        )
                        importedDays.add(wd)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("CalendarImporter", "Permission missing", e)
        } catch (e: Exception) {
            Log.e("CalendarImporter", "Error importing", e)
        }

        return importedDays
    }

    private fun isGardeEvent(title: String): Boolean {
        val keywords = listOf("Garde", "SAMU", "SMUR", "Ambulance", "VSL", "Service")
        return keywords.any { title.contains(it, ignoreCase = true) }
    }
}
