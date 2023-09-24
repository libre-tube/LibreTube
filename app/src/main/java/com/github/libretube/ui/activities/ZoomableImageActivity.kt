package com.github.libretube.ui.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityZoomableImageBinding
import com.github.libretube.extensions.parcelableExtra

/**
 * An activity that allows you to zoom and rotate an image
 */
class ZoomableImageActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityZoomableImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bitmap: Bitmap = intent.parcelableExtra(IntentData.bitmap)!!
        binding.imageView.setImageBitmap(bitmap)
    }
}