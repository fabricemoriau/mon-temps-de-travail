package com.example.un

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.ScanEntity
import com.example.un.utils.GalleryHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 200
    private var currentScanType = "" 
    
    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: FloatingActionButton
    private lateinit var btnCloseCamera: FloatingActionButton
    private lateinit var scrollMenu: View
    private lateinit var tvHint: TextView
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_scanner)

        initViews()
        setupListeners()
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        intent.getStringExtra("SCAN_TYPE")?.let {
            if (it == "PAIE") {
                currentScanType = "PAIE"
                checkPermissionAndLaunchCamera()
            }
        }
    }

    private fun initViews() {
        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        btnCloseCamera = findViewById(R.id.btnCloseCamera)
        scrollMenu = findViewById(R.id.scrollScannerMenu)
        tvHint = findViewById(R.id.tvCameraHint)
    }

    private fun setupListeners() {
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
        
        findViewById<Button>(R.id.btnViewAlbumRoute).setOnClickListener { openAlbum("ROUTE") }
        findViewById<Button>(R.id.btnViewAlbumCarnet).setOnClickListener { openAlbum("CARNET") }
        findViewById<Button>(R.id.btnViewAlbumPaie).setOnClickListener { openAlbum("PAIE") }

        btnCapture.setOnClickListener { takePhoto() }
        btnCloseCamera.setOnClickListener { hideCamera() }
    }

    private fun openAlbum(type: String) {
        val intent = Intent(this, ScanAlbumActivity::class.java)
        intent.putExtra("SCAN_TYPE", type)
        startActivity(intent)
    }

    private fun checkPermissionAndLaunchCamera() {
        if (allPermissionsGranted()) {
            showCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showCamera() {
        scrollMenu.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        btnCapture.visibility = View.VISIBLE
        btnCloseCamera.visibility = View.VISIBLE
        tvHint.visibility = View.VISIBLE
        supportActionBar?.hide()
        
        startCamera()
    }

    private fun hideCamera() {
        scrollMenu.visibility = View.VISIBLE
        viewFinder.visibility = View.GONE
        btnCapture.visibility = View.GONE
        btnCloseCamera.visibility = View.GONE
        tvHint.visibility = View.GONE
        supportActionBar?.show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e("ScannerActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        btnCapture.isEnabled = false
        Toast.makeText(this, "Capture en cours...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) {
                        saveScan(bitmap)
                        hideCamera()
                    }
                    btnCapture.isEnabled = true
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("ScannerActivity", "Photo capture failed: ${exc.message}", exc)
                    btnCapture.isEnabled = true
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Gérer la rotation
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
                val timestamp = System.currentTimeMillis()
                val fileName = "scan_$timestamp"
                
                val subFolder = when(currentScanType) {
                    "ROUTE" -> "Feuilles de route"
                    "CARNET" -> "Carnets"
                    "PAIE" -> "Fiches de paie"
                    else -> "Divers"
                }

                val savedPath = GalleryHelper.saveImageToGallery(this@ScannerActivity, bitmap, fileName, subFolder)

                if (savedPath != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    val dateStr = sdf.format(Date(timestamp))

                    val scan = ScanEntity(
                        id = UUID.randomUUID().toString(),
                        type = currentScanType,
                        imagePath = savedPath,
                        dateFormatted = dateStr,
                        month = month,
                        year = year
                    )

                    AppDatabase.getDatabase(applicationContext).scanDao().insert(scan)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScannerActivity, "Document enregistré dans l'album $subFolder !", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScannerActivity, "Erreur de sauvegarde", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                showCamera()
            } else {
                Toast.makeText(this, "Permissions caméra refusées", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (viewFinder.visibility == View.VISIBLE) {
            hideCamera()
            return true
        }
        finish()
        return true
    }
}
