package com.example.un

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.montempsdetravail.R
import com.example.un.data.LieuCode
import com.example.un.data.LocalDataManager
import java.util.*

class AddLieuActivity : AppCompatActivity() {

    private var lieuId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_lieu)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etNom = findViewById<EditText>(R.id.etLieuNom)
        val etCode = findViewById<EditText>(R.id.etLieuCode)
        val etNotes = findViewById<EditText>(R.id.etLieuNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveLieu)

        lieuId = intent.getStringExtra("LIEU_ID")

        if (lieuId != null) {
            findViewById<TextView>(R.id.tvAddLieuTitle)?.text = "Modifier le Lieu"
            loadLieuData(lieuId!!, etNom, etCode, etNotes)
        }

        btnSave.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val code = etCode.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (nom.isNotEmpty()) {
                val userId = LocalDataManager.getUserId(this)
                val id = lieuId ?: UUID.randomUUID().toString()
                val newLieu = LieuCode(
                    id = id, 
                    nomLieu = nom, 
                    code = code, 
                    notes = notes, 
                    creatorId = userId,
                    lastModified = System.currentTimeMillis()
                )
                
                LocalDataManager.updateLieuLocally(this, newLieu)
                saveLieuToFirebase(newLieu)
            } else {
                Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLieuData(id: String, etNom: EditText, etCode: EditText, etNotes: EditText) {
        LocalDataManager.getSharedFirebaseRef("lieux_codes").child(id).get().addOnSuccessListener { snapshot ->
            val lieu = snapshot.getValue(LieuCode::class.java)
            if (lieu != null) {
                etNom.setText(lieu.nomLieu)
                etCode.setText(lieu.code)
                etNotes.setText(lieu.notes)
            }
        }
    }

    private fun saveLieuToFirebase(lieu: LieuCode) {
        LocalDataManager.getSharedFirebaseRef("lieux_codes").child(lieu.id).setValue(lieu)
            .addOnSuccessListener {
                Toast.makeText(this, "Lieu enregistré !", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
