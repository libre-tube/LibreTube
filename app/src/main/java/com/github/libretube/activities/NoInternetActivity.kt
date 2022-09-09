package com.github.libretube.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.github.libretube.R
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.extensions.getStyledSnackBar
import com.github.libretube.fragments.DownloadsFragment
import com.github.libretube.util.NetworkHelper
import com.github.libretube.util.ThemeHelper

class NoInternetActivity : BaseActivity() {
    private lateinit var binding: ActivityNointernetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNointernetBinding.inflate(layoutInflater)
        // retry button
        binding.retryButton.setOnClickListener {
            if (NetworkHelper.isNetworkAvailable(this)) {
                ThemeHelper.restartMainActivity(this)
            } else {
                binding.root.getStyledSnackBar(R.string.turnInternetOn).show()
            }
        }
        binding.noInternetSettingsImageView.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.downloads.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.noInternet_container, DownloadsFragment())
                .addToBackStack(null)
                .commit()
        }

        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity()
                }
            }
        )
    }
}
