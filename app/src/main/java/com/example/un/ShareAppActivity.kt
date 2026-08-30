package com.example.un

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.montempsdetravail.R

class ShareAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_app)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Partager l'application"

        val ivAndroid = findViewById<ImageView>(R.id.ivQrAndroid)
        val ivIphone = findViewById<ImageView>(R.id.ivQrIphone)

        val androidUrl = "https://github.com/fabricemoriau/mon-temps-de-travail/releases/download/latest/app-debug.apk"
        val iphoneUrl = "https://github.com/fabricemoriau/mon-temps-de-travail/releases/download/latest/shared_framework.zip"

        loadQr(androidUrl, ivAndroid)
        loadQr(iphoneUrl, ivIphone)
    }

    private fun loadQr(data: String, imageView: ImageView) {
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$data"
        Glide.with(this)
            .load(qrUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .into(imageView)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
