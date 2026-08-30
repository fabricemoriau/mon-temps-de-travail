package com.example.un

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.ClientViewModel
import com.example.un.data.ClientViewModelFactory
import kotlinx.coroutines.launch

class ClientsActivity : AppCompatActivity() {

    private val viewModel: ClientViewModel by viewModels {
        ClientViewModelFactory((application as MonTempsApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clients)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarClients)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_clients)

        val adapter = ClientAdapter(
            onEdit = { client ->
                val intent = Intent(this, AddClientActivity::class.java)
                intent.putExtra("CLIENT_ID", client.id)
                startActivity(intent)
            },
            onDelete = { client ->
                confirmDelete(client.id)
            },
            onShare = { client ->
                shareClientInfo(client)
            }
        )

        findViewById<RecyclerView>(R.id.rvClients).apply {
            layoutManager = LinearLayoutManager(this@ClientsActivity)
            this.adapter = adapter
        }

        viewModel.filteredClients.observe(this) { clients ->
            Log.d("ClientsActivity", "Liste reçue du ViewModel : ${clients.size} patients")
            adapter.submitList(clients)
            findViewById<android.view.View>(R.id.tvEmptyState).visibility = 
                if (clients.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        findViewById<EditText>(R.id.etSearchClient).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnAddClient).setOnClickListener {
            startActivity(Intent(this, AddClientActivity::class.java))
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

    private fun confirmDelete(clientId: String) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer la fiche")
            .setMessage("Voulez-vous vraiment masquer cette fiche et la supprimer du partage ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteClient(clientId)
                Toast.makeText(this, getString(R.string.msg_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun shareClientInfo(client: com.example.un.data.local.ClientEntity) {
        val sb = StringBuilder()
        sb.append("📋 Fiche Patient : ${client.nom} ${client.prenom}\n")
        sb.append("----------------------------\n")
        if (client.dateNaissance.isNotEmpty()) {
            sb.append("🎂 Né(e) le : ${client.dateNaissance}\n")
        }
        if (client.tel.isNotEmpty()) {
            sb.append("📞 Tel : ${client.tel}\n")
        }
        if (client.adresse.isNotEmpty()) {
            sb.append("📍 Adresse : ${client.adresse}\n")
        }
        if (client.notes.isNotEmpty()) {
            sb.append("\n📝 Notes : ${client.notes}")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Partager la fiche patient"))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
