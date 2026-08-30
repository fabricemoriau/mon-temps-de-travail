package com.example.un.utils

import java.text.Normalizer
import java.util.regex.Pattern

object SearchUtils {

    /**
     * Supprime les accents et met en minuscules.
     */
    fun normalize(text: String): String {
        val nfdNormalizedString = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase()
    }

    /**
     * Vérifie si un objet contient tous les mots clés fournis.
     */
    fun matches(searchQuery: String, vararg targets: String): Boolean {
        if (searchQuery.isBlank()) return true
        
        val normalizedQuery = normalize(searchQuery)
        val queryWords = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        val combinedTarget = targets.joinToString(" ") { normalize(it) }
        
        return queryWords.all { word ->
            combinedTarget.contains(word)
        }
    }
}
