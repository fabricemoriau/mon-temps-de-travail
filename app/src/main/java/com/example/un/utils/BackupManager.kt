package com.example.un.utils

import android.content.Context
import android.net.Uri
import com.example.un.data.local.AppBackup
import com.example.un.data.local.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Exporte toutes les données de la base vers un fichier JSON.
     */
    suspend fun createBackup(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val backup = AppBackup(
                    clients = db.clientDao().getAllClientsList(),
                    workDays = db.workDayDao().getAllWorkDaysList(),
                    scans = db.scanDao().getAllScansList(),
                    lieux = db.lieuDao().getAllLieuxList(),
                    collegues = db.collegueDao().getAllColleguesList()
                )
                gson.toJson(backup)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Crée un fichier ZIP contenant toutes les photos JPG du dossier de l'application.
     */
    suspend fun createPhotosZip(context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val photosDir = context.getExternalFilesDir(null) ?: return@withContext null
                val zipFile = File(context.cacheDir, "Sauvegarde_Photos_Travail.zip")
                val zipOut = ZipOutputStream(FileOutputStream(zipFile))

                val files = photosDir.listFiles { _, name -> name.endsWith(".jpg") } ?: emptyArray()
                
                files.forEach { file ->
                    val zipEntry = ZipEntry(file.name)
                    zipOut.putNextEntry(zipEntry)
                    val inputStream = FileInputStream(file)
                    inputStream.copyTo(zipOut)
                    inputStream.close()
                    zipOut.closeEntry()
                }
                zipOut.close()
                zipFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Restaure les données à partir d'un URI (fichier sélectionné par l'utilisateur).
     */
    suspend fun restoreBackup(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val reader = InputStreamReader(inputStream)
                val backup = gson.fromJson(reader, AppBackup::class.java)
                reader.close()

                if (backup != null) {
                    val db = AppDatabase.getDatabase(context)
                    
                    // On insère tout (Room gère le OnConflictStrategy.REPLACE par défaut dans mes DAOs)
                    backup.clients.forEach { db.clientDao().insertOrUpdate(it) }
                    backup.workDays.forEach { db.workDayDao().insert(it) }
                    backup.scans.forEach { 
                        // On répare le chemin d'image pour qu'il soit compatible avec le nouveau téléphone
                        val fileName = File(it.imagePath).name
                        val newPath = File(context.getExternalFilesDir(null), fileName).absolutePath
                        db.scanDao().insert(it.copy(imagePath = newPath)) 
                    }
                    backup.lieux.forEach { db.lieuDao().insertOrUpdate(it) }
                    backup.collegues.forEach { db.collegueDao().insertOrUpdate(it) }
                    
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Décompresse un fichier ZIP vers le dossier des photos de l'application.
     */
    suspend fun restorePhotosZip(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val photosDir = context.getExternalFilesDir(null) ?: return@withContext false
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                val zipIn = ZipInputStream(inputStream)
                
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val outFile = File(photosDir, entry.name)
                    val outStream = FileOutputStream(outFile)
                    zipIn.copyTo(outStream)
                    outStream.close()
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                zipIn.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
