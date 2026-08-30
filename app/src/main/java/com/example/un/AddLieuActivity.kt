package com.example.un

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.montempsdetravail.R
import com.example.un.data.*
import com.example.un.data.local.LieuEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.*

class AddLieuActivity : AppCompatActivity() {

    private val viewModel: LieuViewModel by viewModels {
        LieuViewModelFactory((application as MonTempsApp).repository)
    }

    private var lieuId: String? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_lieu)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etNom = findViewById<EditText>(R.id.etLieuNom)
        val etCode = findViewById<EditText>(R.id.etLieuCode)
        val etTel = findViewById<EditText>(R.id.etLieuTel)
        val etAdresse = findViewById<EditText>(R.id.etLieuAdresse)
        val etNotes = findViewById<EditText>(R.id.etLieuNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveLieu)
        val btnGps = findViewById<ImageButton>(R.id.btnLieuGps)

        lieuId = intent.getStringExtra("LIEU_ID")

        if (lieuId != null) {
            findViewById<TextView>(R.id.tvAddLieuTitle)?.text = "Modifier le Lieu"
            viewModel.getLieu(lieuId!!).observe(this) { lieu ->
                lieu?.let {
                    if (!etNom.hasFocus()) etNom.setText(it.nomLieu)
                    if (!etCode.hasFocus()) etCode.setText(it.code)
                    if (!etTel.hasFocus()) etTel.setText(it.tel)
                    if (!etAdresse.hasFocus()) etAdresse.setText(it.adresse)
                    if (!etNotes.hasFocus()) etNotes.setText(it.notes)
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }

        btnGps.setOnClickListener { getLocation() }

        btnSave.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val code = etCode.text.toString().trim()
            val tel = etTel.text.toString().trim()
            val adresse = etAdresse.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (nom.isNotEmpty()) {
                val userId = LocalDataManager.getUserId(this)
                val id = lieuId ?: UUID.randomUUID().toString()
                val newLieu = LieuEntity(
                    id = id, 
                    nomLieu = nom, 
                    code = code, 
                    tel = tel,
                    adresse = adresse,
                    latitude = currentLat,
                    longitude = currentLng,
                    notes = notes, 
                    creatorId = userId,
                    lastModified = System.currentTimeMillis()
                )
                
                viewModel.saveLieu(newLieu)
                Toast.makeText(this, "Lieu enregistré !", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(this)
                Toast.makeText(this, "Localisation en cours...", Toast.LENGTH_SHORT).show()
                
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLat = location.latitude
                            currentLng = location.longitude
                            updateAddressFromGps(location.latitude, location.longitude)
                        } else {
                            Toast.makeText(this, "Position indisponible", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur GPS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAddressFromGps(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                findViewById<EditText>(R.id.etLieuAdresse).setText(addresses[0].getAddressLine(0))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur Geocodeur", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
