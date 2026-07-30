package com.example.un

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.LocalDataManager
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class SharedMessagingActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val sharedRef = FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_SHARED_MESSAGES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_messaging)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messagerie Partagée"

        val rv = findViewById<RecyclerView>(R.id.rvChatMessages)
        val etMsg = findViewById<EditText>(R.id.etChatMessage)
        val btnSend = findViewById<Button>(R.id.btnSendChatMessage)

        adapter = ChatAdapter(messages)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        btnSend.setOnClickListener {
            val text = etMsg.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                etMsg.setText("")
            }
        }

        listenForMessages()
    }

    private fun sendMessage(text: String) {
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val sender = "${prefs.getString("prenom", "")} ${prefs.getString("nom", "")}".trim()
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = if (sender.isEmpty()) "Anonyme" else sender,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        sharedRef.child(msg.id).setValue(msg)
    }

    private fun listenForMessages() {
        sharedRef.orderByChild("timestamp").limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                snapshot.children.forEach { child ->
                    val m = child.getValue(ChatMessage::class.java)
                    if (m != null) messages.add(m)
                }
                adapter.notifyDataSetChanged()
                findViewById<RecyclerView>(R.id.rvChatMessages).scrollToPosition(messages.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
