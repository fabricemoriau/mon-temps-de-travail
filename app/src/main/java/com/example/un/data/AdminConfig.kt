package com.example.un.data

object AdminConfig {
    // Flag à changer manuellement avant de compiler l'APK (true pour Maître, false pour Collègue)
    const val IS_MASTER_VERSION = false 
    
    // Code secret pour les actions sensibles
    const val ADMIN_CODE = "MASTER2026"
    
    // Nœuds Firebase partagés
    const val PATH_SHARED_PATIENTS = "shared/clients"
    const val PATH_SHARED_COLLEGUES = "shared/collegues"
    const val PATH_SHARED_MESSAGES = "shared/messages"
    const val PATH_BLOCKED_USERS = "shared/blocked_ids"
}
