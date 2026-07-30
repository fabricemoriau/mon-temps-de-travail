package com.example.un.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.io.File

object LocalDataManager {

    val gson = Gson()
    private const val TAG = "LocalDataManager"

    fun getUserId(context: Context): String {
        val sharedPref = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val nom = sharedPref.getString("nom", "")?.lowercase()?.trim()?.replace(" ", "_") ?: ""
        val prenom = sharedPref.getString("prenom", "")?.lowercase()?.trim()?.replace(" ", "_") ?: ""
        val tel = sharedPref.getString("tel", "")?.lowercase()?.trim()?.replace(" ", "") ?: ""
        
        return if (nom.isNotEmpty() && prenom.isNotEmpty()) {
            if (tel.isNotEmpty()) "${nom}_${prenom}_$tel" else "${nom}_${prenom}"
        } else {
            "user_anonymous"
        }
    }

    fun getFirebaseRef(context: Context, nodeName: String): DatabaseReference {
        val userId = getUserId(context)
        return FirebaseDatabase.getInstance().getReference("users").child(userId).child(nodeName)
    }

    fun getSharedFirebaseRef(nodeName: String): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference("shared").child(nodeName)
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

    /**
     * Fusionne deux listes en privilégiant le plus récent (via Timestamp).
     */
    fun <T : Any> mergeLists(local: List<T>, remote: List<T>, idSelector: (T) -> String): List<T> {
        val resultMap = mutableMapOf<String, T>()
        local.forEach { 
            val id = idSelector(it)
            if (id.isNotEmpty()) resultMap[id] = it 
        }
        remote.forEach { remoteItem ->
            val id = idSelector(remoteItem)
            if (id.isEmpty()) return@forEach
            
            val localItem = resultMap[id]
            
            if (localItem == null) {
                resultMap[id] = remoteItem
            } else {
                val remoteTs = getTimestamp(remoteItem)
                val localTs = getTimestamp(localItem)
                if (remoteTs > localTs) {
                    resultMap[id] = remoteItem
                }
            }
        }
        return resultMap.values.toList()
    }

    private fun getTimestamp(item: Any): Long {
        return when (item) {
            is com.example.un.data.local.ClientEntity -> item.updatedAt
            is com.example.un.data.LieuCode -> item.lastModified
            else -> 0L
        }
    }

    fun loadLieux(context: Context): List<LieuCode> = loadIndividualItems(context, "lieux_codes")
    fun updateLieuLocally(context: Context, lieu: LieuCode) = saveIndividualItem(context, "lieux_codes", lieu.id, lieu)
    fun loadCollegues(context: Context): List<Collegue> = loadIndividualItems(context, "collegues")
    fun updateCollegueLocally(context: Context, col: Collegue) = saveIndividualItem(context, "collegues", col.id, col)
    fun loadDocs(context: Context): List<DocumentObligatoire> = loadIndividualItems(context, "docs")
    fun updateDocLocally(context: Context, doc: DocumentObligatoire) = saveIndividualItem(context, "docs", doc.id, doc)
}
