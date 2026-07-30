package com.example.un

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.montempsdetravail.R
import com.example.un.data.DocumentObligatoire
import com.example.un.data.LocalDataManager
import java.util.*

class AddDocActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentImageUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_doc)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etTitle = findViewById<EditText>(R.id.etDocTitre)
        val etExp = findViewById<EditText>(R.id.etDocDate)
        val btnScan = findViewById<Button>(R.id.btnScanDoc)
        val btnSave = findViewById<Button>(R.id.btnSaveDoc)

        ViewUtils.addDateFormatter(etExp)

        btnScan.setOnClickListener {
            checkPermissionAndLaunchCamera()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                val doc = DocumentObligatoire(
                    id = UUID.randomUUID().toString(),
                    titre = title,
                    dateExpiration = etExp.text.toString(),
                    imageUri = currentImageUri
                )
                LocalDataManager.updateDocLocally(this, doc)
                Toast.makeText(this, "Document sauvegardé", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            currentImageUri = "https://via.placeholder.com/600x800?text=Scanner"
            Toast.makeText(this, "Scan terminé", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
