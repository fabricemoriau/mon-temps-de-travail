package com.example.un

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.Collegue
import com.example.un.data.LocalDataManager
import com.google.firebase.database.FirebaseDatabase

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
            val nom = etNom?.text?.toString()?.trim() ?: ""
            val prenom = etPrenom?.text?.toString()?.trim() ?: ""
            val tel = etTel?.text?.toString()?.trim() ?: ""
            val societe = etSociete?.text?.toString()?.trim() ?: ""

            if (nom.isNotEmpty() && prenom.isNotEmpty()) {
                // 1. Sauvegarde locale
                with(sharedPref.edit()) {
                    putString("nom", nom)
                    putString("prenom", prenom)
                    putString("societe", societe)
                    putString("anciennete", etAnciennete?.text?.toString() ?: "")
                    putString("poste", etPoste?.text?.toString() ?: "")
                    putString("adresse_perso", etAdressePerso?.text?.toString() ?: "")
                    putString("adresse_travail", etAdresseTravail?.text?.toString() ?: "")
                    putString("tel", tel)
                    putBoolean("is_profile_complete", true)
                    apply()
                }

                // 2. Synchronisation Firebase (Identification automatique comme collègue)
                syncToSharedCollegues(nom, prenom, tel)

                Toast.makeText(this, "Profil enregistré et partagé !", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Veuillez remplir au moins votre nom et prénom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncToSharedCollegues(nom: String, prenom: String, tel: String) {
        val userId = LocalDataManager.getUserId(this)
        val societe = getSharedPreferences("UserProfile", MODE_PRIVATE).getString("societe", "") ?: ""
        
        val col = Collegue(
            id = userId,
            nom = nom,
            prenom = prenom,
            tel = if (societe.isNotEmpty()) "$tel ($societe)" else tel
        )
        FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_SHARED_COLLEGUES)
            .child(userId)
            .setValue(col)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
