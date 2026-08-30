package com.example.un.data

/**
 * Modèle pour les informations de mise à jour sur Firebase
 */
data class AppUpdateInfo(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val downloadUrl: String = "",
    val updateMessage: String = "Une nouvelle version de l'application est disponible.",
    val forceUpdate: Boolean = false
)
