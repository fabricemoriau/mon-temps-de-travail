package com.example.un.data.local

/**
 * Objet global regroupant toutes les données de l'application pour la sauvegarde.
 */
data class AppBackup(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val clients: List<ClientEntity> = emptyList(),
    val workDays: List<WorkDayEntity> = emptyList(),
    val scans: List<ScanEntity> = emptyList(),
    val lieux: List<LieuEntity> = emptyList(),
    val collegues: List<CollegueEntity> = emptyList()
)
