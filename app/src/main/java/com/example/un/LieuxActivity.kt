package com.example.un

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.LieuCode
import com.example.un.data.LocalDataManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class LieuxActivity : AppCompatActivity() {

    private lateinit var adapter: LieuAdapter
    private val lieuxList = mutableListOf<LieuCode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lieux)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_lieux)

        val rv = findViewById<RecyclerView>(R.id.rvLieux)
        rv.layoutManager = LinearLayoutManager(this)
        
        setupFirebaseListener()

        findViewById<Button>(R.id.btnAddLieu).setOnClickListener {
            startActivity(Intent(this, AddLieuActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupFirebaseListener() {
        LocalDataManager.getSharedFirebaseRef("lieux_codes")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteLieux = mutableListOf<LieuCode>()
                    val remoteIds = mutableSetOf<String>()
                    
                    for (child in snapshot.children) {
                        val lieu = child.getValue(LieuCode::class.java)
                        if (lieu != null && lieu.id.isNotEmpty()) {
                            remoteLieux.add(lieu)
                            remoteIds.add(lieu.id)
                        }
                    }
                    
                    val localLieux = LocalDataManager.loadLieux(this@LieuxActivity)
                    
                    // 1. Supprimer localement ce qui n'est plus sur Firebase
                    localLieux.forEach { local ->
                        if (!remoteIds.contains(local.id)) {
                            LocalDataManager.deleteIndividualItem(this@LieuxActivity, "lieux_codes", local.id)
                        }
                    }
                    
                    // 2. Fusionner
                    val finalList = LocalDataManager.mergeLists(localLieux.filter { remoteIds.contains(it.id) }, remoteLieux) { it.id }
                    
                    // 3. Sauvegarde locale miroir
                    finalList.forEach { LocalDataManager.updateLieuLocally(this@LieuxActivity, it) }
                    
                    refreshList()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun refreshList() {
        lieuxList.clear()
        lieuxList.addAll(LocalDataManager.loadLieux(this).sortedBy { it.nomLieu.lowercase() })
        
        adapter = LieuAdapter(lieuxList, { lieu ->
            val intent = Intent(this, AddLieuActivity::class.java)
            intent.putExtra("LIEU_ID", lieu.id)
            startActivity(intent)
        }, { lieu ->
            confirmDelete(lieu)
        })
        findViewById<RecyclerView>(R.id.rvLieux).adapter = adapter
    }

    private fun confirmDelete(lieu: LieuCode) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le code")
            .setMessage("Voulez-vous supprimer ce lieu du téléphone et du partage ?")
            .setPositiveButton("Supprimer") { _, _ ->
                LocalDataManager.getSharedFirebaseRef("lieux_codes").child(lieu.id).removeValue()
                LocalDataManager.deleteIndividualItem(this, "lieux_codes", lieu.id)
                refreshList()
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
