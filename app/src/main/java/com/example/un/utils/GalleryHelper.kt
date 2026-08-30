package com.example.un.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object GalleryHelper {

    /**
     * Enregistre une image dans la galerie publique sous "Pictures/Mes heures de travail/[subFolder]"
     */
    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String, subFolder: String): String? {
        val relativePath = "Pictures/Mes heures de travail/$subFolder"
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            
            val contentResolver = context.contentResolver
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                it.toString()
            }
        } else {
            // Version plus ancienne d'Android
            val directory = File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile, relativePath)
            if (!directory.exists()) directory.mkdirs()
            
            val file = File(directory, "$fileName.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        }
    }
}
