package com.github.libretube.ui.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.github.libretube.R
import com.github.libretube.databinding.ActivitySettingsBinding
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.ui.preferences.MainSettings

class SettingsActivity : BaseActivity() {
    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.backImageButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainSettings())
                .commit()
        }

        // new way of dealing with back presses instead of onBackPressed()
        onBackPressedDispatcher.addCallback(
            this, // lifecycle owner
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when (supportFragmentManager.findFragmentById(R.id.settings)) {
                        is MainSettings -> {
                            finishAndRemoveTask()
                        }
                        else -> {
                            supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.settings, MainSettings())
                                .commit()
                            changeTopBarText(getString(R.string.settings))
                        }
                    }
                }
            }
        )
    }

    fun changeTopBarText(text: String) {
        if (this::binding.isInitialized) binding.topBarTextView.text = text
    }
}
