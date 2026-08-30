package com.example.un

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.montempsdetravail.R

class ImageDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        val ivFullImage = findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        val imagePath = intent.getStringExtra("IMAGE_PATH")

        if (imagePath != null) {
            Glide.with(this)
                .load(imagePath)
                .into(ivFullImage)
        }

        btnClose.setOnClickListener { finish() }
    }
}
