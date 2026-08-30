package com.example.un

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.example.un.utils.GalleryHelper
import java.util.*

class AddDocActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private var currentImageUri: String = ""
    private var lastBitmap: Bitmap? = null
    private var photoFile: java.io.File? = null

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
            if (title.isNotEmpty() && lastBitmap != null) {
                val timestamp = System.currentTimeMillis()
                val fileName = "doc_$timestamp"
                val savedPath = GalleryHelper.saveImageToGallery(this, lastBitmap!!, fileName, "Documents obligatoires")
                
                if (savedPath != null) {
                    val doc = DocumentObligatoire(
                        id = UUID.randomUUID().toString(),
                        titre = title,
                        dateExpiration = etExp.text.toString(),
                        imageUri = savedPath
                    )
                    LocalDataManager.updateDocLocally(this, doc)
                    Toast.makeText(this, "Document sauvegardé dans l'album !", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else if (title.isEmpty()) {
                Toast.makeText(this, "Entrez un titre", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Veuillez scanner le document", Toast.LENGTH_SHORT).show()
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
                try {
                    photoFile = java.io.File.createTempFile("TEMP_DOC_", ".jpg", cacheDir)
                    val photoURI = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        photoFile!!
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (e: Exception) {
                    Toast.makeText(this, "Erreur caméra", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (photoFile != null && photoFile!!.exists()) {
                lastBitmap = android.graphics.BitmapFactory.decodeFile(photoFile!!.absolutePath)
                if (lastBitmap != null) {
                    Toast.makeText(this, "Scan terminé, n'oubliez pas d'enregistrer", Toast.LENGTH_SHORT).show()
                }
                photoFile?.delete()
            } else {
                val imageBitmap = data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    lastBitmap = imageBitmap
                    Toast.makeText(this, "Scan terminé, n'oubliez pas d'enregistrer", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
