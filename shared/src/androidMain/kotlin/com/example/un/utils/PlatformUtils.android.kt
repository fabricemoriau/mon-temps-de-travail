package com.example.un.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

// On utilise un holder pour injecter le context android
object AndroidContext {
    var context: Context? = null
}

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidContext.context?.startActivity(intent)
}
