package com.example.un.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val nom: String = "",
    val prenom: String = "",
    val tel: String = "",
    val adresse: String = "",
    val notes: String = "",
    val dateNaissance: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Champs de synchronisation professionnelle
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false, // false = doit être envoyé à Firebase
    val creatorId: String = ""
)
