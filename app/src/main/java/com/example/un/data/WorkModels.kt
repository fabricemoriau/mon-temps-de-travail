package com.example.un.data

data class WorkDay(
    val date: Long, // timestamp
    val vehiculeType: String,
    val amplitudeMillis: Long,
    val effectiveMillis: Long,
    val nightMillis: Long,
    val pauseMillis: Long,
    val startHour: Int,
    val endHour: Int,
    val hasExtraRepas: Boolean = false,
    val pauseNuitMillis: Long = 0,
    val isPauseExterieur: Boolean = true,
    val isGardeJour: Boolean = false,
    val isGardeNuit: Boolean = false
)
