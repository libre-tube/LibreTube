package com.github.libretube.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.util.ThemeHelper
import com.google.android.material.color.DynamicColors

class NoInternetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNointernetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        ThemeHelper.updateTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityNointernetBinding.inflate(layoutInflater)
        binding.retryButton.setOnClickListener {
            ThemeHelper.restartMainActivity(this)
        }
        binding.noInternetSettingsImageView.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        finishAffinity()
        super.onBackPressed()
    }
}
