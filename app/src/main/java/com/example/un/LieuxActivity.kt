package com.example.un

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.LieuViewModel
import com.example.un.data.LieuViewModelFactory
import kotlinx.coroutines.launch

class LieuxActivity : AppCompatActivity() {

    private val viewModel: LieuViewModel by viewModels {
        LieuViewModelFactory((application as MonTempsApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lieux)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_lieux)

        val rv = findViewById<RecyclerView>(R.id.rvLieux)
        rv.layoutManager = LinearLayoutManager(this)
        
        val adapter = LieuAdapter(mutableListOf(), { lieu ->
            val intent = Intent(this, AddLieuActivity::class.java)
            intent.putExtra("LIEU_ID", lieu.id)
            startActivity(intent)
        }, { lieu ->
            confirmDelete(lieu.id)
        })
        rv.adapter = adapter

        findViewById<android.widget.EditText>(R.id.etSearchLieu).addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase().trim()
                viewModel.allLieux.value?.let { lieux ->
                    val filtered = lieux.filter { 
                        it.nomLieu.lowercase().contains(query) || it.code.lowercase().contains(query) || it.adresse.lowercase().contains(query)
                    }.map { 
                        com.example.un.data.LieuCode(
                            it.id, it.nomLieu, it.code, it.adresse, it.tel, it.notes, 
                            it.latitude, it.longitude, it.creatorId, it.lastModified
                        )
                    }
                    adapter.updateList(filtered)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        viewModel.allLieux.observe(this) { lieux ->
            // On convertit LieuEntity en LieuCode pour l'adapter (compatibilité)
            val legacyList = lieux.map { 
                com.example.un.data.LieuCode(
                    it.id, it.nomLieu, it.code, it.adresse, it.tel, it.notes, 
                    it.latitude, it.longitude, it.creatorId, it.lastModified
                )
            }
            adapter.updateList(legacyList)
        }

        findViewById<Button>(R.id.btnAddLieu).setOnClickListener {
            startActivity(Intent(this, AddLieuActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sync, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_sync) {
            lifecycleScope.launch {
                (application as MonTempsApp).repository.forceSyncAll()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmDelete(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le code")
            .setMessage("Voulez-vous supprimer ce lieu du téléphone et du partage ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteLieu(id)
                Toast.makeText(this, "Code supprimé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
