package com.example.un.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.un.data.AdminConfig
import com.example.un.data.AppUpdateInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Gère la vérification des mises à jour via Firebase
 */
object AppUpdateManager {

    fun checkUpdates(activity: Activity, manualCheck: Boolean = false) {
        val database = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
        val updateRef = database.getReference(AdminConfig.PATH_APP_UPDATE)

        // On utilise a simple listener pour la vérification manuelle pour éviter le spam
        updateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val updateInfo = snapshot.getValue(AppUpdateInfo::class.java)
                    if (updateInfo != null) {
                        compareVersionAndNotify(activity, updateInfo, manualCheck)
                    }
                } else if (manualCheck) {
                    Toast.makeText(activity, "Impossible de vérifier les mises à jour", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (manualCheck) {
                    Toast.makeText(activity, "Erreur réseau : ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun compareVersionAndNotify(activity: Activity, info: AppUpdateInfo, manualCheck: Boolean) {
        val currentVersionCode = try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }

        if (info.latestVersionCode > currentVersionCode) {
            showUpdateDialog(activity, info)
        } else if (manualCheck) {
            Toast.makeText(activity, "L'application est déjà à jour (v${info.latestVersionName})", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Publie la version actuelle comme étant la dernière disponible sur Firebase.
     * Réservé au mode Maître.
     */
    fun publishCurrentVersion(activity: Activity) {
        val database = FirebaseDatabase.getInstance(AdminConfig.FIREBASE_URL)
        val updateRef = database.getReference(AdminConfig.PATH_APP_UPDATE)

        try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            val name = pInfo.versionName

            val updateInfo = AppUpdateInfo(
                latestVersionCode = code,
                latestVersionName = name ?: "1.0",
                downloadUrl = "https://votre-lien-de-telechargement.com/final_2.apk", // À personnaliser
                updateMessage = "Une nouvelle version stable ($name) est disponible avec des améliorations majeures.",
                forceUpdate = false
            )

            updateRef.setValue(updateInfo).addOnSuccessListener {
                Toast.makeText(activity, "Version $name publiée avec succès !", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(activity, "Échec publication : ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Erreur lecture version : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpdateDialog(activity: Activity, info: AppUpdateInfo) {
        if (activity.isFinishing || activity.isDestroyed) return

        val builder = AlertDialog.Builder(activity)
            .setTitle("Mise à jour disponible (${info.latestVersionName})")
            .setMessage(info.updateMessage)
            .setCancelable(!info.forceUpdate)
            .setPositiveButton("Mettre à jour") { _, _ ->
                openDownloadUrl(activity, info.downloadUrl)
            }

        if (!info.forceUpdate) {
            builder.setNegativeButton("Plus tard", null)
        }

        builder.show()
    }

    private fun openDownloadUrl(context: Context, url: String) {
        if (url.isEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Échec d'ouverture du navigateur
        }
    }
}
