package com.github.libretube.ui.activities

import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.github.libretube.R
import com.github.libretube.databinding.ActivitySettingsBinding
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.preferences.MainSettings

class SettingsActivity : BaseActivity() {
    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainSettings>(R.id.settings)
            }
        }

        // new way of dealing with back presses instead of onBackPressed()
        onBackPressedDispatcher.addCallback(this) {
            if (supportFragmentManager.findFragmentById(R.id.settings) is MainSettings) {
                finishAndRemoveTask()
            } else {
                supportFragmentManager.commit {
                    replace<MainSettings>(R.id.settings)
                }
                changeTopBarText(getString(R.string.settings))
            }
        }
    }

    fun changeTopBarText(text: String) {
        if (this::binding.isInitialized) binding.toolbar.title = text
    }
}
