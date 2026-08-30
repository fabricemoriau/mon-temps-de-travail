package com.example.un.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
@Entity(tableName = "lieux")
data class LieuEntity(
    @PrimaryKey val id: String = "",
    val nomLieu: String = "",
    val code: String = "",
    val adresse: String = "",
    val tel: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val creatorId: String = "",
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
    val isSynced: Boolean = false
)
