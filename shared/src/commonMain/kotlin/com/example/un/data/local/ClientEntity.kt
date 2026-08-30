package com.example.un.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey 
    val id: String = "", 
    val nom: String = "",
    val prenom: String = "",
    val tel: String = "",
    val adresse: String = "",
    val notes: String = "",
    val dateNaissance: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Champs de synchronisation professionnelle
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val creatorId: String = ""
)
