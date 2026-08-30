package com.example.un

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.*
import com.example.un.data.local.CollegueEntity
import com.example.un.utils.BackupManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private val viewModel: CollegueViewModel by viewModels {
        CollegueViewModelFactory((application as MonTempsApp).repository)
    }

    private val pickBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { restoreDatabase(it) }
    }

    private val pickPhotosLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { restorePhotos(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_profile)

        val etNom = findViewById<EditText>(R.id.etNom)
        val etPrenom = findViewById<EditText>(R.id.etPrenom)
        val etSociete = findViewById<EditText>(R.id.etSociete)
        val etAnciennete = findViewById<EditText>(R.id.etAnciennete)
        val etPoste = findViewById<EditText>(R.id.etPoste)
        val etAdressePerso = findViewById<EditText>(R.id.etAdressePerso)
        val etAdresseTravail = findViewById<EditText>(R.id.etAdresseTravail)
        val etTel = findViewById<EditText>(R.id.etTel)
        val etAdminCode = findViewById<EditText>(R.id.etAdminCode)
        val etBaseHeures = findViewById<EditText>(R.id.etBaseHeures)
        val etTauxNetBase = findViewById<EditText>(R.id.etTauxNetBase)
        val etMajorNuit = findViewById<EditText>(R.id.etMajorNuit)
        val etPrimeDimanche = findViewById<EditText>(R.id.etPrimeDimanche)
        val etPrimeGardeSamu = findViewById<EditText>(R.id.etPrimeGardeSamu)
        val etTauxPanierProf = findViewById<EditText>(R.id.etTauxPanier)
        val cbCalcMensuel = findViewById<CheckBox>(R.id.cbCalcMensuel)
        val btnSave = findViewById<Button>(R.id.btnSave)
        
        val cbDea = findViewById<CheckBox>(R.id.cbDeaCca)
        val cbTaxi = findViewById<CheckBox>(R.id.cbTaxis)
        val cbAux = findViewById<CheckBox>(R.id.cbAuxiliaire)
        val cbAutre = findViewById<CheckBox>(R.id.cbAutre)

        findViewById<Button>(R.id.btnExportBackup).setOnClickListener { exportDatabase() }
        findViewById<Button>(R.id.btnExportPhotos).setOnClickListener { exportPhotos() }
        findViewById<Button>(R.id.btnRestoreBackup).setOnClickListener { 
            pickBackupLauncher.launch(arrayOf("application/json", "*/*")) 
        }
        findViewById<Button>(R.id.btnRestorePhotos).setOnClickListener { 
            pickPhotosLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) 
        }

        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        if (AdminConfig.isMaster(this)) {
            etAdminCode?.setHint("MODE MAÎTRE ACTIVÉ")
        }

        etNom?.setText(sharedPref.getString("nom", ""))
        etPrenom?.setText(sharedPref.getString("prenom", ""))
        etSociete?.setText(sharedPref.getString("societe", ""))
        etAnciennete?.setText(sharedPref.getString("anciennete", ""))
        etPoste?.setText(sharedPref.getString("poste", ""))
        etAdressePerso?.setText(sharedPref.getString("adresse_perso", ""))
        etAdresseTravail?.setText(sharedPref.getString("adresse_travail", ""))
        etTel?.setText(sharedPref.getString("tel", ""))
        etBaseHeures?.setText(sharedPref.getString("base_heures", "151.67"))
        etTauxNetBase?.setText(sharedPref.getString("taux_net_base", "12.50"))
        etMajorNuit?.setText(sharedPref.getString("major_nuit", "25"))
        etPrimeDimanche?.setText(sharedPref.getString("prime_dimanche", "26.30"))
        etPrimeGardeSamu?.setText(sharedPref.getString("prime_garde_samu", "30.00"))
        etTauxPanierProf?.setText(sharedPref.getString("taux_panier_prof", "9.20"))
        cbCalcMensuel?.isChecked = sharedPref.getBoolean("calc_mensuel", false)
        
        cbDea?.isChecked = sharedPref.getBoolean("qualif_dea", false)
        cbTaxi?.isChecked = sharedPref.getBoolean("qualif_taxi", false)
        cbAux?.isChecked = sharedPref.getBoolean("qualif_aux", false)
        cbAutre?.isChecked = sharedPref.getBoolean("qualif_autre", false)

        btnSave?.setOnClickListener {
            val nom = etNom?.text?.toString()?.trim() ?: ""
            val prenom = etPrenom?.text?.toString()?.trim() ?: ""
            val tel = etTel?.text?.toString()?.trim() ?: ""
            val societe = etSociete?.text?.toString()?.trim() ?: ""
            val codeEntered = etAdminCode?.text?.toString()?.trim() ?: ""

            if (nom.isNotEmpty() && prenom.isNotEmpty()) {
                with(sharedPref.edit()) {
                    putString("nom", nom)
                    putString("prenom", prenom)
                    putString("societe", societe)
                    putString("tel", tel)
                    putString("base_heures", etBaseHeures?.text?.toString() ?: "151.67")
                    putString("taux_net_base", etTauxNetBase?.text?.toString() ?: "12.50")
                    putString("major_nuit", etMajorNuit?.text?.toString() ?: "25")
                    putString("prime_dimanche", etPrimeDimanche?.text?.toString() ?: "26.30")
                    putString("prime_garde_samu", etPrimeGardeSamu?.text?.toString() ?: "30.00")
                    putString("taux_panier_prof", etTauxPanierProf?.text?.toString() ?: "9.20")
                    putBoolean("calc_mensuel", cbCalcMensuel?.isChecked ?: false)
                    putBoolean("is_profile_complete", true)
                    
                    putBoolean("qualif_dea", cbDea?.isChecked ?: false)
                    putBoolean("qualif_taxi", cbTaxi?.isChecked ?: false)
                    putBoolean("qualif_aux", cbAux?.isChecked ?: false)
                    putBoolean("qualif_autre", cbAutre?.isChecked ?: false)
                    
                    apply()
                }

                if (codeEntered == AdminConfig.ADMIN_CODE) {
                    AdminConfig.setMasterMode(this, true)
                    Toast.makeText(this, "MODE MAÎTRE DÉBLOQUÉ !", Toast.LENGTH_LONG).show()
                }

                val userId = LocalDataManager.getUserId(this)
                val collegue = CollegueEntity(
                    id = userId,
                    nom = nom,
                    prenom = prenom,
                    tel = if (societe.isNotEmpty()) "$tel ($societe)" else tel,
                    lastModified = System.currentTimeMillis()
                )
                viewModel.saveCollegue(collegue)

                Toast.makeText(this, "Profil enregistré et partagé !", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Veuillez remplir au moins votre nom et prénom", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportDatabase() {
        lifecycleScope.launch {
            val json = BackupManager.createBackup(this@ProfileActivity)
            if (json != null) {
                val file = File(cacheDir, "Sauvegarde_Donnees_Travail.json")
                FileOutputStream(file).use { it.write(json.toByteArray()) }
                shareFile(file, "application/json")
            } else {
                Toast.makeText(this@ProfileActivity, "Échec de l'exportation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportPhotos() {
        lifecycleScope.launch {
            Toast.makeText(this@ProfileActivity, "Préparation du dossier photos...", Toast.LENGTH_LONG).show()
            val zipFile = BackupManager.createPhotosZip(this@ProfileActivity)
            if (zipFile != null && zipFile.exists()) {
                shareFile(zipFile, "application/zip")
            } else {
                Toast.makeText(this@ProfileActivity, "Aucune photo à exporter ou échec", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreDatabase(uri: Uri) {
        lifecycleScope.launch {
            val success = BackupManager.restoreBackup(this@ProfileActivity, uri)
            if (success) {
                Toast.makeText(this@ProfileActivity, "Données restaurées avec succès !", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ProfileActivity, "Échec de la restauration des données", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restorePhotos(uri: Uri) {
        lifecycleScope.launch {
            val success = BackupManager.restorePhotosZip(this@ProfileActivity, uri)
            if (success) {
                Toast.makeText(this@ProfileActivity, "Photos restaurées avec succès !", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ProfileActivity, "Échec de la restauration des photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Sauvegarder vers..."))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
