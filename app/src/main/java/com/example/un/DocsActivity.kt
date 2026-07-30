package com.example.un

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.LocalDataManager

class DocsActivity : AppCompatActivity() {

    private lateinit var adapter: DocAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_docs)

        val rv = findViewById<RecyclerView>(R.id.rvDocs)
        rv.layoutManager = LinearLayoutManager(this)

        refreshList()

        findViewById<Button>(R.id.btnAddDoc).setOnClickListener {
            startActivity(Intent(this, AddDocActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val docs = LocalDataManager.loadDocs(this)
        adapter = DocAdapter(docs) { doc ->
            // Detail logic
        }
        findViewById<RecyclerView>(R.id.rvDocs).adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
