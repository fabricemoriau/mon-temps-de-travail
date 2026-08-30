package com.example.un

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.*

class MessageGroupActivity : AppCompatActivity() {

    private val viewModel: CollegueViewModel by viewModels {
        CollegueViewModelFactory((application as MonTempsApp).repository)
    }

    private val colleguesList = mutableListOf<com.example.un.data.Collegue>()
    private val selectedIds = mutableSetOf<String>()
    private lateinit var adapter: SelectableCollegueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_group)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Message Groupé"

        val etMessage = findViewById<EditText>(R.id.etMessageContent)
        val rv = findViewById<RecyclerView>(R.id.rvSelectCollegues)
        
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SelectableCollegueAdapter(colleguesList) { id, isChecked ->
            if (isChecked) selectedIds.add(id) else selectedIds.remove(id)
        }
        rv.adapter = adapter

        viewModel.allCollegues.observe(this) { collegues ->
            colleguesList.clear()
            colleguesList.addAll(collegues.map { 
                com.example.un.data.Collegue(it.id, it.nom, it.prenom, it.tel)
            })
            adapter.notifyDataSetChanged()
        }

        findViewById<Button>(R.id.btnSendToAll).setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty()) {
                sendSmsTo(colleguesList.map { it.tel }, msg)
            } else {
                Toast.makeText(this, "Tapez un message !", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSendToSelected).setOnClickListener {
            val msg = etMessage.text.toString()
            val selectedTels = colleguesList.filter { selectedIds.contains(it.id) }.map { it.tel }
            if (msg.isNotEmpty() && selectedTels.isNotEmpty()) {
                sendSmsTo(selectedTels, msg)
            } else {
                Toast.makeText(this, "Sélectionnez des collègues et tapez un message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSmsTo(numbers: List<String>, message: String) {
        // Nettoyer les numéros pour enlever les noms de sociétés entre parenthèses
        // et ne garder que les caractères valides pour un numéro
        val cleanNumbers = numbers.map { it.substringBefore("(").filter { c -> c.isDigit() || c == '+' } }
        val numbersString = cleanNumbers.joinToString(";")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$numbersString")
            putExtra("sms_body", message)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
