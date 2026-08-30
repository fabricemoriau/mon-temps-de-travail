package com.example.un.data

data class LieuCode(
    val id: String = "",
    val nomLieu: String = "",
    val code: String = "",
    val adresse: String = "",
    val tel: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val creatorId: String = "",
    val lastModified: Long = 0L
)

data class DocumentObligatoire(
    val id: String = "",
    val titre: String = "",
    val dateExpiration: String? = null,
    val imageUri: String = "",
    val type: String = "DOCUMENT"
)

data class Collegue(
    val id: String = "",
    val nom: String = "",
    val prenom: String = "",
    val tel: String = ""
)
