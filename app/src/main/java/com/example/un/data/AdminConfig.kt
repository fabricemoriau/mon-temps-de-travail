package com.example.un.data

import android.content.Context

object AdminConfig {
    // Mode Maître : Peut être forcé ici pour le développement, 
    // ou activé dynamiquement via le code secret dans le profil.
    private const val FORCED_MASTER_MODE = false 

    /**
     * Vérifie si l'utilisateur actuel a les droits Maître.
     * Priorité au flag forcé, sinon vérifie les SharedPreferences.
     */
    fun isMaster(context: Context): Boolean {
        if (FORCED_MASTER_MODE) return true
        val prefs = context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_master", false)
    }

    /**
     * Active ou désactive le mode Maître sur cet appareil.
     */
    fun setMasterMode(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_master", active).apply()
    }
    
    // URL DE VOTRE BASE DE DONNÉES (Remplacez si nécessaire)
    const val FIREBASE_URL = "https://mon-temps-de-travail-default-rtdb.europe-west1.firebasedatabase.app"
    
    // Code secret pour les actions sensibles et l'activation du mode Maître
    const val ADMIN_CODE = "MASTER2026"
    
    // Nœuds Firebase partagés
    const val PATH_SHARED_PATIENTS = "shared/clients"
    const val PATH_SHARED_COLLEGUES = "shared/collegues"
    const val PATH_SHARED_MESSAGES = "shared/messages"
    const val PATH_BLOCKED_USERS = "shared/blocked_ids"
    
    // Chemin pour les mises à jour de l'application
    const val PATH_APP_UPDATE = "shared/app_config/update_info"
}
