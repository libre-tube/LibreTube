package com.github.libretube.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.databinding.ActivitySettingsBinding
import com.github.libretube.preferences.MainSettings
import com.github.libretube.util.ThemeHelper
import com.google.android.material.color.DynamicColors

class SettingsActivity : AppCompatActivity() {
    val TAG = "SettingsActivity"
    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        ThemeHelper.updateTheme(this)

        // makes the preference dialogs use material dialogs
        setTheme(R.style.MaterialAlertDialog)

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            overridePendingTransition(50, 50)
        }
        binding.root.alpha = 0F
        binding.root.animate().alpha(1F).duration = 300

        setContentView(binding.root)

        binding.backImageButton.setOnClickListener {
            onBackPressed()
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainSettings())
                .commit()
        }
    }

    override fun onBackPressed() {
        if (Globals.isCurrentViewMainSettings) {
            super.onBackPressed()
            finishAndRemoveTask()
        } else {
            Globals.isCurrentViewMainSettings = true
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainSettings())
                .commit()
            changeTopBarText(getString(R.string.settings))
        }
    }

    fun changeTopBarText(text: String) {
        if (this::binding.isInitialized) binding.topBarTextView.text = text
    }
}
