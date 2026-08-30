package com.example.un

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.*
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ColleguesActivity : AppCompatActivity() {

    private val viewModel: CollegueViewModel by viewModels {
        CollegueViewModelFactory((application as MonTempsApp).repository)
    }

    private lateinit var adapter: CollegueAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collegues)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mes Collègues"

        val rv = findViewById<RecyclerView>(R.id.rvCollegues)
        rv.layoutManager = LinearLayoutManager(this)
        
        adapter = CollegueAdapter(mutableListOf(), { showAddDialog(it) }, { confirmDelete(it) })
        rv.adapter = adapter

        viewModel.allCollegues.observe(this) { collegues ->
            val legacyList = collegues.map { 
                Collegue(it.id, it.nom, it.prenom, it.tel)
            }
            adapter.updateList(legacyList)
        }

        findViewById<Button>(R.id.btnAddCollegue).setOnClickListener { showAddDialog() }
        
        findViewById<Button>(R.id.btnGoToMyProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnGoToMessaging).setOnClickListener {
            startActivity(Intent(this, MessageGroupActivity::class.java))
        }

        val btnAdmin = findViewById<Button>(R.id.btnAdminPanel)
        if (AdminConfig.isMaster(this)) {
            btnAdmin.visibility = View.VISIBLE
            btnAdmin.setOnClickListener { showAdminPanel() }
        }

        findViewById<EditText>(R.id.etSearchCollegue).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { 
                val query = s.toString().lowercase().trim()
                viewModel.allCollegues.value?.let { list ->
                    val filtered = list.filter { 
                        it.nom.lowercase().contains(query) || it.prenom.lowercase().contains(query) || it.tel.contains(query)
                    }.map { 
                        Collegue(it.id, it.nom, it.prenom, it.tel)
                    }
                    adapter.updateList(filtered)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showAddDialog(colToEdit: Collegue? = null) {
        val view = layoutInflater.inflate(R.layout.dialog_add_collegue, null)
        val etNom = view.findViewById<EditText>(R.id.etColNom)
        val etPrenom = view.findViewById<EditText>(R.id.etColPrenom)
        val etTel = view.findViewById<EditText>(R.id.etColTel)

        colToEdit?.let {
            etNom.setText(it.nom)
            etPrenom.setText(it.prenom)
            etTel.setText(it.tel)
        }

        AlertDialog.Builder(this)
            .setTitle(if (colToEdit == null) "Ajouter un Collègue" else "Modifier le Collègue")
            .setView(view)
            .setPositiveButton("Enregistrer") { _, _ ->
                val nom = etNom.text.toString().trim()
                val prenom = etPrenom.text.toString().trim()
                val tel = etTel.text.toString().trim()
                if (nom.isNotEmpty()) {
                    val id = colToEdit?.id ?: UUID.randomUUID().toString()
                    val c = com.example.un.data.local.CollegueEntity(id, nom, prenom, tel)
                    viewModel.saveCollegue(c)
                } else {
                    Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmDelete(collegue: Collegue) {
        if (!AdminConfig.isMaster(this)) {
            Toast.makeText(this, "Seul l'administrateur peut supprimer un collègue partagé.", Toast.LENGTH_LONG).show()
            return
        }

        val etCode = EditText(this)
        etCode.hint = "Code Administrateur"
        etCode.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Suppression Protégée")
            .setMessage("Entrez le code pour supprimer ${collegue.prenom} de Firebase :")
            .setView(etCode)
            .setPositiveButton("Supprimer") { _, _ ->
                if (etCode.text.toString() == AdminConfig.ADMIN_CODE) {
                    FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference(AdminConfig.PATH_SHARED_COLLEGUES).child(collegue.id).removeValue()
                    Toast.makeText(this, "Supprimé de Firebase", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAdminPanel() {
        // ... (Logique admin panel reste similaire, mais observe la DB locale si besoin)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
