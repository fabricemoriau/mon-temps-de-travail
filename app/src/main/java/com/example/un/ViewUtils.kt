package com.example.un

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object ViewUtils {

    /**
     * Ajoute un formateur automatique de date (JJ/MM/AAAA) à un EditText.
     */
    fun addDateFormatter(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private var oldText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                val currentText = s.toString()
                
                // Si on supprime, on ne fait rien pour ne pas bloquer l'utilisateur
                if (currentText.length < oldText.length) return

                val formatted = formatAsDate(currentText)
                if (formatted != currentText) {
                    isUpdating = true
                    val selectionStart = editText.selectionStart
                    editText.setText(formatted)
                    
                    // Repositionnement intelligent du curseur
                    var newSelection = selectionStart
                    if (formatted.length > currentText.length) {
                        // Si un slash a été inséré juste avant ou à la position du curseur
                        if (selectionStart == 2 || selectionStart == 5) {
                            newSelection++
                        }
                    }
                    editText.setSelection(newSelection.coerceIn(0, formatted.length))
                    isUpdating = false
                }
            }

            private fun formatAsDate(input: String): String {
                val clean = input.replace("/", "")
                val sb = StringBuilder()
                
                for (i in clean.indices) {
                    sb.append(clean[i])
                    if ((i == 1 || i == 3) && i < clean.length - 1) {
                        sb.append("/")
                    }
                }
                
                // Limiter à 10 caractères (JJ/MM/AAAA)
                return if (sb.length > 10) sb.substring(0, 10) else sb.toString()
            }
        })
    }
}
