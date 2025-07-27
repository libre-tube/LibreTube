package com.github.libretube.ui.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.github.libretube.R
import com.github.libretube.databinding.ActivitySettingsBinding
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.preferences.InstanceSettings
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
            goToMainSettings()
        }

        handleRedirect()
    }

    fun goToMainSettings() {
        redirectTo<MainSettings>()
        changeTopBarText(getString(R.string.settings))
    }

    private fun handleRedirect() {
        val redirectKey = intent.extras?.getString(REDIRECT_KEY)

        if (redirectKey == REDIRECT_TO_INTENT_SETTINGS) redirectTo<InstanceSettings>()
    }

    fun changeTopBarText(text: String) {
        if (this::binding.isInitialized) binding.toolbar.title = text
    }

    private inline fun <reified T : Fragment> redirectTo() {
        supportFragmentManager.commit {
            replace<T>(R.id.settings)
        }
    }

    companion object {
        const val REDIRECT_KEY = "redirect"
        const val REDIRECT_TO_INTENT_SETTINGS = "intent_settings"
    }
}
