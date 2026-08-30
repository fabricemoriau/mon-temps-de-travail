package com.example.un

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.ForumTopic
import com.example.un.data.LocalDataManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import java.util.*

class ForumActivity : AppCompatActivity() {

    private val topics = mutableListOf<ForumTopic>()
    private lateinit var adapter: ForumAdapter
    private val forumRef = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference("shared/forum/topics")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Forum de discussion"

        val currentUserId = LocalDataManager.getUserId(this)
        val isMaster = AdminConfig.isMaster(this)

        val rv = findViewById<RecyclerView>(R.id.rvForumTopics)
        adapter = ForumAdapter(topics, currentUserId, isMaster, { selectedTopic ->
            val intent = Intent(this, SharedMessagingActivity::class.java)
            intent.putExtra("TOPIC_ID", selectedTopic.id)
            intent.putExtra("TOPIC_TITLE", selectedTopic.title)
            startActivity(intent)
        }, { topicToDelete ->
            confirmDeleteTopic(topicToDelete)
        })

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddTopic).setOnClickListener {
            showAddTopicDialog(currentUserId)
        }

        listenForTopics()
    }

    private fun listenForTopics() {
        forumRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                topics.clear()
                snapshot.children.forEach { child ->
                    child.getValue(ForumTopic::class.java)?.let { topics.add(0, it) } // Plus récent en haut
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ForumActivity, "Erreur forum : ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddTopicDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_collegue, null) 
        val etTitle = dialogView.findViewById<EditText>(R.id.etColNom)
        etTitle.hint = "Titre du sujet"
        
        // Cacher les autres champs
        dialogView.findViewById<android.view.View>(R.id.etColPrenom).visibility = android.view.View.GONE
        dialogView.findViewById<android.view.View>(R.id.etColTel).visibility = android.view.View.GONE

        AlertDialog.Builder(this)
            .setTitle("Nouveau sujet")
            .setView(dialogView)
            .setPositiveButton("Créer") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    createNewTopic(title, userId)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun createNewTopic(title: String, userId: String) {
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val sender = "${prefs.getString("prenom", "")} ${prefs.getString("nom", "")}".trim()
        
        val topic = ForumTopic(
            id = UUID.randomUUID().toString(),
            title = title,
            creatorId = userId,
            creatorName = if (sender.isEmpty()) "Anonyme" else sender,
            timestamp = System.currentTimeMillis()
        )
        
        forumRef.child(topic.id).setValue(topic).addOnSuccessListener {
            Toast.makeText(this, "Sujet créé !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteTopic(topic: ForumTopic) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le sujet")
            .setMessage("Voulez-vous supprimer '${topic.title}' et tous ses messages ?")
            .setPositiveButton("Supprimer") { _, _ ->
                forumRef.child(topic.id).removeValue()
                // Supprimer aussi les messages associés
                FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
                    .getReference("shared/forum/messages")
                    .child(topic.id)
                    .removeValue()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
