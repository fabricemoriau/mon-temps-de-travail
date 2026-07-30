package com.example.un

import android.app.AlertDialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.ScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScannerActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private val PERMISSION_REQUEST_CODE = 200
    private var currentScanType = "" // "ROUTE" or "CARNET"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_scanner)

        findViewById<Button>(R.id.btnScanRoute).setOnClickListener {
            currentScanType = "ROUTE"
            checkPermissionAndLaunchCamera()
        }

        findViewById<Button>(R.id.btnScanCarnet).setOnClickListener {
            currentScanType = "CARNET"
            checkPermissionAndLaunchCamera()
        }

        findViewById<Button>(R.id.btnScanPaie).setOnClickListener {
            currentScanType = "PAIE"
            checkPermissionAndLaunchCamera()
        }
        
        findViewById<Button>(R.id.btnViewAlbumRoute).setOnClickListener {
            openAlbum("ROUTE")
        }
        
        findViewById<Button>(R.id.btnViewAlbumCarnet).setOnClickListener {
            openAlbum("CARNET")
        }

        findViewById<Button>(R.id.btnViewAlbumPaie).setOnClickListener {
            openAlbum("PAIE")
        }

        intent.getStringExtra("SCAN_TYPE")?.let {
            if (it == "PAIE") {
                currentScanType = "PAIE"
                Toast.makeText(this, "Mode Scan Feuille de Paie", Toast.LENGTH_SHORT).show()
                checkPermissionAndLaunchCamera()
            }
        }
    }

    private fun openAlbum(type: String) {
        val intent = Intent(this, ScanAlbumActivity::class.java)
        intent.putExtra("SCAN_TYPE", type)
        startActivity(intent)
    }

    private fun checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } ?: Toast.makeText(this, getString(R.string.msg_camera_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                saveScan(imageBitmap)
            }
        }
    }

    private fun saveScan(bitmap: Bitmap) {
        if (currentScanType == "PAIE") {
            showDatePickerAndSave(bitmap)
        } else {
            processAndSave(bitmap, null, null)
        }
    }

    private fun showDatePickerAndSave(bitmap: Bitmap) {
        val cal = Calendar.getInstance()
        val months = arrayOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Mois du bulletin de paie")
        builder.setItems(months) { _, which ->
            val month = which + 1
            val year = cal.get(Calendar.YEAR)
            processAndSave(bitmap, month, year)
        }
        builder.show()
    }

    private fun processAndSave(bitmap: Bitmap, month: Int?, year: Int?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "scan_${System.currentTimeMillis()}.jpg"
                val file = File(getExternalFilesDir(null), fileName)
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                val dateStr = sdf.format(Date())

                val scan = ScanEntity(
                    id = UUID.randomUUID().toString(),
                    type = currentScanType,
                    imagePath = file.absolutePath,
                    dateFormatted = dateStr,
                    month = month,
                    year = year
                )

                AppDatabase.getDatabase(applicationContext).scanDao().insert(scan)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScannerActivity, "Document enregistré !", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScannerActivity, "Erreur de sauvegarde", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
