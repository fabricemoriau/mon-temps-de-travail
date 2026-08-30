package com.example.un.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
@Entity(tableName = "collegues")
data class CollegueEntity(
    @PrimaryKey val id: String = "",
    val nom: String = "",
    val prenom: String = "",
    val tel: String = "",
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
    val isSynced: Boolean = false
)
