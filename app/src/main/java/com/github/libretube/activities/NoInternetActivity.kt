package com.github.libretube.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.R
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.snackbar.Snackbar

class NoInternetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNointernetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.updateTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityNointernetBinding.inflate(layoutInflater)
        // retry button
        binding.retryButton.setOnClickListener {
            if (ConnectionHelper.isNetworkAvailable(this)) {
                ThemeHelper.restartMainActivity(this)
            } else {
                val snackBar = Snackbar
                    .make(binding.root, R.string.turnInternetOn, Snackbar.LENGTH_LONG)
                snackBar.show()
            }
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
