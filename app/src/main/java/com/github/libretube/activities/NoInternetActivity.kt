package com.github.libretube.activities

import android.content.Intent
import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.snackbar.Snackbar

class NoInternetActivity : BaseActivity() {
    private lateinit var binding: ActivityNointernetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
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
