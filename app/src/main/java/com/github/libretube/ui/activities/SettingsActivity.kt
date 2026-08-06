package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.github.libretube.R
import com.github.libretube.databinding.ActivitySettingsBinding
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.preferences.InstanceSettings

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val navController = binding.settings.getFragment<NavHostFragment>().navController
        setSupportActionBar(binding.toolbar)
        setContentView(binding.root)

        // ensure that the toolbar's back button is always visible
        val appBarConfiguration = AppBarConfiguration.Builder()
            .setFallbackOnNavigateUpListener {
                finish()
                true
            }
            .build()
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        if (intent.extras?.getString(REDIRECT_KEY) == REDIRECT_TO_INTENT_SETTINGS) {
            navController.navigate(R.id.action_global_instanceSettings)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data!!.host == "login_callback") {
            val token = intent.data?.getQueryParameter("token")
            if (token == null) {
                toastFromMainThread(R.string.error)
                return
            }
            PreferenceHelper.setToken(token)
        } else if (intent.data!!.path == "delete_callback") {
            PreferenceHelper.setToken("")
        }

        // update visible login/logout settings
        binding.settings.getFragment<NavHostFragment>().childFragmentManager.fragments.filterIsInstance<InstanceSettings>()
            .firstOrNull()?.toggleAuthAccountActionsUI(true)
    }

    companion object {
        const val REDIRECT_KEY = "redirect"
        const val REDIRECT_TO_INTENT_SETTINGS = "intent_settings"
    }
}
