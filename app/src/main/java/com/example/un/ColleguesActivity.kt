package com.example.un

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.Collegue
import com.example.un.data.LocalDataManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class ColleguesActivity : AppCompatActivity() {

    private lateinit var adapter: CollegueAdapter
    private val colleguesList = mutableListOf<Collegue>()
    private val filteredList = mutableListOf<Collegue>()
    
    private val sharedRef = FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_SHARED_COLLEGUES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collegues)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mes Collègues"

        val rv = findViewById<RecyclerView>(R.id.rvCollegues)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = CollegueAdapter(filteredList, { showAddDialog(it) }, { confirmDelete(it) })
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAddCollegue).setOnClickListener { showAddDialog() }
        
        findViewById<Button>(R.id.btnGoToMessaging).setOnClickListener {
            startActivity(Intent(this, MessageGroupActivity::class.java))
        }

        val btnAdmin = findViewById<Button>(R.id.btnAdminPanel)
        if (AdminConfig.IS_MASTER_VERSION) {
            btnAdmin.visibility = View.VISIBLE
            btnAdmin.setOnClickListener { showAdminPanel() }
        }

        findViewById<EditText>(R.id.etSearchCollegue).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        startFirebaseSync()
    }

    private fun startFirebaseSync() {
        sharedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                colleguesList.clear()
                snapshot.children.forEach { child ->
                    val c = child.getValue(Collegue::class.java)
                    if (c != null) colleguesList.add(c)
                }
                filter(findViewById<EditText>(R.id.etSearchCollegue)?.text?.toString() ?: "")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filter(text: String) {
        filteredList.clear()
        val query = text.lowercase().trim()
        if (query.isEmpty()) {
            filteredList.addAll(colleguesList)
        } else {
            for (c in colleguesList) {
                if (c.nom.lowercase().contains(query) || c.prenom.lowercase().contains(query)) {
                    filteredList.add(c)
                }
            }
        }
        adapter.notifyDataSetChanged()
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
                    val c = Collegue(id, nom, prenom, tel)
                    sharedRef.child(id).setValue(c)
                } else {
                    Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmDelete(collegue: Collegue) {
        if (!AdminConfig.IS_MASTER_VERSION) {
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
                    sharedRef.child(collegue.id).removeValue()
                    Toast.makeText(this, "Supprimé de Firebase", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAdminPanel() {
        val dialogView: android.view.View = layoutInflater.inflate(R.layout.dialog_admin_panel, null)
        val etUserId = dialogView.findViewById<EditText>(R.id.etBlockUserId)
        val btnBlock = dialogView.findViewById<Button>(R.id.btnBlockUser)
        val btnUnblock = dialogView.findViewById<Button>(R.id.btnUnblockUser)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Administration Maître")
            .setView(dialogView)
            .setNegativeButton("Fermer", null)
            .create()

        btnBlock.setOnClickListener {
            val id = etUserId.text.toString().trim().lowercase()
            if (id.isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_BLOCKED_USERS).child(id).setValue(true)
                Toast.makeText(this, "Utilisateur $id BLOQUÉ", Toast.LENGTH_SHORT).show()
            }
        }

        btnUnblock.setOnClickListener {
            val id = etUserId.text.toString().trim().lowercase()
            if (id.isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_BLOCKED_USERS).child(id).removeValue()
                Toast.makeText(this, "Utilisateur $id DÉBLOQUÉ", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
