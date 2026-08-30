package com.example.un.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val id: String,
    val type: String, // "ROUTE", "CARNET" ou "PAIE"
    val imagePath: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val dateFormatted: String, // Format "dd/MM/yyyy"
    val month: Int? = null,    // 1-12 pour tri PAIE
    val year: Int? = null      // YYYY pour tri PAIE
)
