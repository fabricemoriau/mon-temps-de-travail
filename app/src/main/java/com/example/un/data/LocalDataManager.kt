package com.example.un.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.io.File
import java.util.UUID

object LocalDataManager {

    val gson = Gson()
    private const val TAG = "LocalDataManager"

    fun getUserId(context: Context): String {
        val sharedPref = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val nom = sharedPref.getString("nom", "")?.lowercase()?.trim()?.replace(Regex("[^a-z0-9]"), "") ?: ""
        val prenom = sharedPref.getString("prenom", "")?.lowercase()?.trim()?.replace(Regex("[^a-z0-9]"), "") ?: ""
        val tel = sharedPref.getString("tel", "")?.lowercase()?.trim()?.replace(Regex("[^0-9]"), "") ?: ""
        
        return if (nom.isNotEmpty() && prenom.isNotEmpty() && tel.isNotEmpty()) {
            "${nom}_${prenom}_$tel"
        } else {
            val tempId = sharedPref.getString("temp_device_id", UUID.randomUUID().toString())
            sharedPref.edit().putString("temp_device_id", tempId).apply()
            tempId!!.replace(Regex("[^a-z0-9-]"), "")
        }
    }

    fun getSharedFirebaseRef(nodeName: String): DatabaseReference {
        return FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL).getReference("shared").child(nodeName)
    }

    fun getDataSubDir(context: Context, subDirName: String): File {
        val dir = File(context.getExternalFilesDir(null), subDirName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun <T> saveIndividualItem(context: Context, subDirName: String, id: String, item: T) {
        try {
            val file = File(getDataSubDir(context, subDirName), "$id.json")
            file.writeText(gson.toJson(item))
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde", e)
        }
    }

    inline fun <reified T> loadIndividualItems(context: Context, subDirName: String): List<T> {
        val list = mutableListOf<T>()
        try {
            val files = getDataSubDir(context, subDirName).listFiles { _, name -> name.endsWith(".json") }
            files?.forEach { file ->
                try {
                    val item = gson.fromJson(file.readText(), T::class.java)
                    if (item != null) list.add(item)
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
        return list
    }

    fun deleteIndividualItem(context: Context, subDirName: String, id: String) {
        try {
            val file = File(getDataSubDir(context, subDirName), "$id.json")
            if (file.exists()) file.delete()
        } catch (e: Exception) {}
    }

    // Utile pour les documents obligatoires (restent en local file pour le moment)
    fun loadDocs(context: Context): List<DocumentObligatoire> = loadIndividualItems(context, "docs")
    fun updateDocLocally(context: Context, doc: DocumentObligatoire) = saveIndividualItem(context, "docs", doc.id, doc)
}
