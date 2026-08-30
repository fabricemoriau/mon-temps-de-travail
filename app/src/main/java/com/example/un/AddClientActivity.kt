package com.example.un

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.montempsdetravail.R
import com.example.un.data.ClientViewModel
import com.example.un.data.ClientViewModelFactory
import com.example.un.data.local.ClientEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.UUID

class AddClientActivity : AppCompatActivity() {

    private val viewModel: ClientViewModel by viewModels {
        ClientViewModelFactory((application as MonTempsApp).repository)
    }

    private var clientId: String? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_client)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etNom = findViewById<EditText>(R.id.etClientNom)
        val etPrenom = findViewById<EditText>(R.id.etClientPrenom)
        val etTel = findViewById<EditText>(R.id.etClientTel)
        val etDateNaissance = findViewById<EditText>(R.id.etClientDateNaissance)
        val etAdresse = findViewById<EditText>(R.id.etClientAdresse)
        val etNotes = findViewById<EditText>(R.id.etClientNotes)
        val cbDecedee = findViewById<CheckBox>(R.id.cbIsDecedee)
        val btnSave = findViewById<Button>(R.id.btnSaveClient)
        val btnGps = findViewById<Button>(R.id.btnGpsLocation)

        clientId = intent.getStringExtra("CLIENT_ID")

        clientId?.let { id ->
            viewModel.getClient(id).observe(this) { client ->
                client?.let {
                    // On ne met à jour que si les champs sont vides (chargement initial)
                    // OU si on veut une synchronisation forcée en temps réel.
                    // Pour éviter de couper la saisie de l'utilisateur, on peut vérifier le focus.
                    if (!etNom.hasFocus()) etNom.setText(it.nom)
                    if (!etPrenom.hasFocus()) etPrenom.setText(it.prenom)
                    if (!etTel.hasFocus()) etTel.setText(it.tel)
                    if (!etDateNaissance.hasFocus()) etDateNaissance.setText(it.dateNaissance)
                    if (!etAdresse.hasFocus()) etAdresse.setText(it.adresse)
                    if (!etNotes.hasFocus()) etNotes.setText(it.notes)
                    if (!cbDecedee.hasFocus()) cbDecedee.isChecked = it.isDeleted
                    
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }

        etDateNaissance.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                val input = s.toString().replace("/", "")
                if (input.length > 8) {
                    isUpdating = true
                    s?.delete(s.length - 1, s.length)
                    isUpdating = false
                    return
                }

                val formatted = StringBuilder()
                for (i in input.indices) {
                    formatted.append(input[i])
                    if ((i == 1 || i == 3) && i != input.length - 1) {
                        formatted.append("/")
                    }
                }

                isUpdating = true
                etDateNaissance.setText(formatted.toString())
                etDateNaissance.setSelection(formatted.length)
                isUpdating = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnGps.setOnClickListener { getLocation() }

        btnSave.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()

            if (nom.isNotEmpty()) {
                val id = clientId ?: UUID.randomUUID().toString()
                
                val client = ClientEntity(
                    id = id,
                    nom = nom,
                    prenom = prenom,
                    tel = etTel.text.toString().trim(),
                    dateNaissance = etDateNaissance.text.toString().trim(),
                    adresse = etAdresse.text.toString().trim(),
                    notes = etNotes.text.toString().trim(),
                    latitude = currentLat,
                    longitude = currentLng,
                    isDeleted = cbDecedee.isChecked, // Reste en mémoire mais marqué décédé
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.saveClient(client)
                Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "Impossible de récupérer la position", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Échec de localisation", Toast.LENGTH_SHORT).show()
                    }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Service GPS indisponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAddressFromGps(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                findViewById<EditText>(R.id.etClientAdresse).setText(addresses[0].getAddressLine(0))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de localisation", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
