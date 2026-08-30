package com.example.un

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.local.DatabaseHelper
import com.example.un.data.local.ScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScanAlbumActivity : AppCompatActivity() {

    private lateinit var adapter: ScanAdapter
    private var scanType: String = "ROUTE"
    private val calendar = Calendar.getInstance()
    private val dateFilter = androidx.lifecycle.MutableLiveData<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_album)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        scanType = intent.getStringExtra("SCAN_TYPE") ?: "ROUTE"
        
        val title = when (scanType) {
            "ROUTE" -> "Feuilles de Route"
            "CARNET" -> "Carnets d'Heures"
            "PAIE" -> "Fiches de Paie"
            else -> "Documents"
        }
        findViewById<TextView>(R.id.tvAlbumTitle).text = title

        val rv = findViewById<RecyclerView>(R.id.rvScans)
        adapter = ScanAdapter { scan ->
            val intent = Intent(this, ImageDetailActivity::class.java)
            intent.putExtra("IMAGE_PATH", scan.imagePath)
            startActivity(intent)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val etSearch = findViewById<EditText>(R.id.etSearchDate)
        etSearch.setOnClickListener { showDatePicker(etSearch) }

        val etStart = findViewById<EditText>(R.id.etStartDate)
        val etEnd = findViewById<EditText>(R.id.etEndDate)
        etStart.setOnClickListener { showDatePicker(etStart) }
        etEnd.setOnClickListener { showDatePicker(etEnd) }

        findViewById<Button>(R.id.btnGeneratePdf).setOnClickListener {
            generatePdf(etStart.text.toString(), etEnd.text.toString())
        }

        observeScans()
    }

    private fun observeScans() {
        dateFilter.observe(this) { date ->
            if (date == null) {
                DatabaseHelper.getDatabase(this).scanDao().getScansByType(scanType).asLiveData().observe(this) {
                    adapter.submitList(it)
                }
            } else {
                DatabaseHelper.getDatabase(this).scanDao().searchByDate(scanType, date).asLiveData().observe(this) {
                    adapter.submitList(it)
                }
            }
        }
    }

    private fun loadScans() {
        dateFilter.value = null
    }

    private fun showDatePicker(editText: EditText) {
        DatePickerDialog(this, { _, y, m, d ->
            val date = String.format(Locale.FRANCE, "%02d/%02d/%d", d, m + 1, y)
            editText.setText(date)
            if (editText.id == R.id.etSearchDate) {
                dateFilter.value = date
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun searchByDate(date: String) {
        // Obsolete
    }

    private fun generatePdf(startStr: String, endStr: String) {
        if (startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(this, "Sélectionnez une période", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                val start = sdf.parse(startStr)?.time ?: 0L
                val calendarEnd = Calendar.getInstance().apply {
                    time = sdf.parse(endStr) ?: Date()
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val end = calendarEnd.timeInMillis

                val scans = DatabaseHelper.getDatabase(this@ScanAlbumActivity).scanDao().getScansInRange(scanType, start, end)
                
                if (scans.isEmpty()) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@ScanAlbumActivity, "Aucun doc trouvé pour cette période", Toast.LENGTH_SHORT).show() }
                    return@launch
                }

                val pdfDocument = PdfDocument()
                val pageWidth = 595 // A4 width in points
                val pageHeight = 842 // A4 height in points

                scans.forEachIndexed { index, scan ->
                    val bitmap = loadResizedBitmap(scan.imagePath, pageWidth, pageHeight) ?: return@forEachIndexed
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    
                    // Calculer le ratio pour que l'image tienne dans la page A4 sans déformation
                    val scaleX = pageWidth.toFloat() / bitmap.width
                    val scaleY = pageHeight.toFloat() / bitmap.height
                    val scale = Math.min(scaleX, scaleY)
                    
                    val left = (pageWidth - bitmap.width * scale) / 2
                    val top = (pageHeight - bitmap.height * scale) / 2
                    
                    val matrix = android.graphics.Matrix()
                    matrix.postScale(scale, scale)
                    matrix.postTranslate(left, top)
                    
                    page.canvas.drawBitmap(bitmap, matrix, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }

                val fileName = "Export_${scanType}_${System.currentTimeMillis()}.pdf"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    sharePdf(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    Toast.makeText(this@ScanAlbumActivity, "Erreur lors de la création du PDF : ${e.message}", Toast.LENGTH_LONG).show() 
                }
            }
        }
    }

    private fun loadResizedBitmap(path: String, maxWidth: Int, maxHeight: Int): android.graphics.Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } else {
            BitmapFactory.decodeFile(path, options)
        }

        var inSampleSize = 1
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } else {
            BitmapFactory.decodeFile(path, options)
        }
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Partager le document PDF"))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
