package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.NetworkHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.DownloadsFragment
import com.google.android.material.snackbar.Snackbar

class NoInternetActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityNointernetBinding.inflate(layoutInflater)
        // retry button
        binding.retryButton.setOnClickListener {
            if (NetworkHelper.isNetworkAvailable(this)) {
                NavigationHelper.restartMainActivity(this)
            } else {
                Snackbar.make(binding.root, R.string.turnInternetOn, Snackbar.LENGTH_LONG).show()
            }
        }
        binding.noInternetSettingsImageView.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.downloads.setOnClickListener {
            supportFragmentManager.commit {
                replace<DownloadsFragment>(R.id.container)
                addToBackStack(null)
            }
        }

        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) {
            supportFragmentManager.fragments.filterIsInstance<DownloadsFragment>()
                .firstOrNull()
                ?.let {
                    supportFragmentManager.commit {
                        remove(it)
                    }
                }
                ?: finishAffinity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.getBooleanExtra(IntentData.openAudioPlayer, false)) {
            // attempt to recycle already existing audio player fragment first before creating new one
            supportFragmentManager.fragments.filterIsInstance<AudioPlayerFragment>().firstOrNull()?.let {
                it.binding.playerMotionLayout.transitionToStart()
                return
            }
            NavigationHelper.startAudioPlayer(this)
        }
    }
}
