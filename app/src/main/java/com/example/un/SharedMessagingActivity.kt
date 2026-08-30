package com.example.un

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.Collegue
import com.example.un.data.LocalDataManager
import com.example.un.utils.NotificationHelper
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class SharedMessagingActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private val collegues = mutableListOf<Collegue>()
    private lateinit var adapter: ChatAdapter
    private lateinit var collegueAdapter: ChatCollegueHeaderAdapter
    
    private var topicId: String = ""
    private lateinit var sharedRef: DatabaseReference
    private val colRef = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference(AdminConfig.PATH_SHARED_COLLEGUES)

    private var isSoundEnabled = true
    private var isActivityVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_messaging)

        topicId = intent.getStringExtra("TOPIC_ID") ?: "global"
        val topicTitle = intent.getStringExtra("TOPIC_TITLE") ?: "Discussion"
        
        sharedRef = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
            .getReference("shared/forum/messages")
            .child(topicId)

        val currentUserId = LocalDataManager.getUserId(this)
        
        // Demander la permission de notification pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Charger la préférence de son
        val prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        isSoundEnabled = prefs.getBoolean("sound_enabled", true)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = topicTitle

        val rv = findViewById<RecyclerView>(R.id.rvChatMessages)
        val rvCol = findViewById<RecyclerView>(R.id.rvChatCollegues)
        val etMsg = findViewById<EditText>(R.id.etChatMessage)
        val btnSend = findViewById<View>(R.id.btnSendChatMessage)
        val cbSound = findViewById<CheckBox>(R.id.cbToggleSound)

        cbSound.isChecked = isSoundEnabled
        cbSound.setOnCheckedChangeListener { _, isChecked ->
            isSoundEnabled = isChecked
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        adapter = ChatAdapter(messages, currentUserId)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        collegueAdapter = ChatCollegueHeaderAdapter(collegues) { selected ->
            val currentText = etMsg.text.toString()
            val mention = "@${selected.prenom} "
            etMsg.setText(currentText + mention)
            etMsg.setSelection(etMsg.text.length)
            etMsg.requestFocus()
        }
        rvCol.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCol.adapter = collegueAdapter

        btnSend.setOnClickListener {
            val text = etMsg.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text, currentUserId)
                etMsg.setText("")
            }
        }

        listenForMessages(currentUserId)
        listenForCollegues()
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        NotificationHelper.clearNotifications(this)
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
    }

    private fun sendMessage(text: String, userId: String) {
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val sender = "${prefs.getString("prenom", "")} ${prefs.getString("nom", "")}".trim()
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = userId,
            senderName = if (sender.isEmpty()) "Anonyme" else sender,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        sharedRef.child(msg.id).setValue(msg)
    }

    private fun listenForMessages(currentUserId: String) {
        sharedRef.orderByChild("timestamp").limitToLast(100).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val m = snapshot.getValue(ChatMessage::class.java)
                if (m != null) {
                    messages.add(m)
                    adapter.notifyItemInserted(messages.size - 1)
                    findViewById<RecyclerView>(R.id.rvChatMessages).scrollToPosition(messages.size - 1)

                    // Alerte sonore et notification
                    if (m.senderId != currentUserId) {
                        if (isSoundEnabled) {
                            NotificationHelper.playNotificationSound(this@SharedMessagingActivity)
                        }
                        
                        if (!isActivityVisible) {
                            NotificationHelper.showNotification(
                                this@SharedMessagingActivity,
                                m.senderName,
                                m.text
                            )
                        }
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForCollegues() {
        colRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                collegues.clear()
                snapshot.children.forEach { child ->
                    child.getValue(Collegue::class.java)?.let { collegues.add(it) }
                }
                collegueAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
