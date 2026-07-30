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
import com.example.un.data.local.AppDatabase
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
        adapter = ScanAdapter { /* Clic sur image */ }
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

        loadScans()
    }

    private fun loadScans() {
        AppDatabase.getDatabase(this).scanDao().getScansByType(scanType).asLiveData().observe(this) {
            adapter.submitList(it)
        }
    }

    private fun showDatePicker(editText: EditText) {
        DatePickerDialog(this, { _, y, m, d ->
            val date = String.format(Locale.FRANCE, "%02d/%02d/%d", d, m + 1, y)
            editText.setText(date)
            if (editText.id == R.id.etSearchDate) {
                searchByDate(date)
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun searchByDate(date: String) {
        AppDatabase.getDatabase(this).scanDao().searchByDate(scanType, date).asLiveData().observe(this) {
            adapter.submitList(it)
        }
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
                val end = (sdf.parse(endStr)?.time ?: 0L) + 86400000 // +1 jour pour inclure la fin

                val scans = AppDatabase.getDatabase(this@ScanAlbumActivity).scanDao().getScansInRange(scanType, start, end)
                
                if (scans.isEmpty()) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@ScanAlbumActivity, "Aucun doc trouvé", Toast.LENGTH_SHORT).show() }
                    return@launch
                }

                val pdfDocument = PdfDocument()
                scans.forEachIndexed { index, scan ->
                    val bitmap = BitmapFactory.decodeFile(scan.imagePath) ?: return@forEachIndexed
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                }

                val fileName = "Export_${scanType}_${System.currentTimeMillis()}.pdf"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    sharePdf(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@ScanAlbumActivity, "Erreur PDF", Toast.LENGTH_SHORT).show() }
            }
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
