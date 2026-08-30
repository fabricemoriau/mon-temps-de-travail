package com.example.un.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_days")
data class WorkDayEntity(
    @PrimaryKey
    val dateId: String, // Format "yyyy-MM-dd"
    val timestamp: Long,
    val vehiculeType: String = "",
    val vehiculeNum: String = "",
    val startMillis: Long = 0,
    val endMillis: Long = 0,
    val pause1Start: Long = 0,
    val pause1End: Long = 0,
    val repasStart: Long = 0,
    val repasEnd: Long = 0,
    val pause2Start: Long = 0,
    val pause2End: Long = 0,
    val pauseNuitStart: Long = 0,
    val pauseNuitEnd: Long = 0,
    val amplitudeMillis: Long = 0,
    val effectiveMillis: Long = 0,
    val nightMillis: Long = 0,
    val supMillis: Long = 0,
    val hasExtraRepas: Boolean = false,
    val isGardeJour: Boolean = false,
    val isGardeNuit: Boolean = false,
    val isAllSup: Boolean = false,
    val isRTT: Boolean = false,
    val isVacation: Boolean = false
)
