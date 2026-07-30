package com.example.un

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.montempsdetravail.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_profile)

        val etNom = findViewById<EditText>(R.id.etNom)
        val etPrenom = findViewById<EditText>(R.id.etPrenom)
        val etSociete = findViewById<EditText>(R.id.etSociete)
        val etAnciennete = findViewById<EditText>(R.id.etAnciennete)
        val etPoste = findViewById<EditText>(R.id.etPoste)
        val etAdressePerso = findViewById<EditText>(R.id.etAdressePerso)
        val etAdresseTravail = findViewById<EditText>(R.id.etAdresseTravail)
        val etTel = findViewById<EditText>(R.id.etTel)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        etNom?.setText(sharedPref.getString("nom", ""))
        etPrenom?.setText(sharedPref.getString("prenom", ""))
        etSociete?.setText(sharedPref.getString("societe", ""))
        etAnciennete?.setText(sharedPref.getString("anciennete", ""))
        etPoste?.setText(sharedPref.getString("poste", ""))
        etAdressePerso?.setText(sharedPref.getString("adresse_perso", ""))
        etAdresseTravail?.setText(sharedPref.getString("adresse_travail", ""))
        etTel?.setText(sharedPref.getString("tel", ""))

        btnSave?.setOnClickListener {
            val nom = etNom?.text?.toString() ?: ""
            val prenom = etPrenom?.text?.toString() ?: ""

            if (nom.isNotEmpty() && prenom.isNotEmpty()) {
                with(sharedPref.edit()) {
                    putString("nom", nom)
                    putString("prenom", prenom)
                    putString("societe", etSociete?.text?.toString() ?: "")
                    putString("anciennete", etAnciennete?.text?.toString() ?: "")
                    putString("poste", etPoste?.text?.toString() ?: "")
                    putString("adresse_perso", etAdressePerso?.text?.toString() ?: "")
                    putString("adresse_travail", etAdresseTravail?.text?.toString() ?: "")
                    putString("tel", etTel?.text?.toString() ?: "")
                    putBoolean("is_profile_complete", true)
                    apply()
                }

                Toast.makeText(this, "Profil enregistré !", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Veuillez remplir au moins votre nom et prénom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
