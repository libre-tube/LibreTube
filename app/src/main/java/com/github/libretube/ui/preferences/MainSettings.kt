package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.util.UpdateChecker
import kotlinx.coroutines.launch

class MainSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val update = findPreference<Preference>("update")
        val appVersion = BuildConfig.VERSION_NAME
        update?.summary = "v${appVersion}"

        // manual trigger, in case
        update?.setOnPreferenceClickListener {

            lifecycleScope.launch {
                val updater = UpdateChecker(requireContext())
                updater.checkUpdate(manualTrigger = true)
            }

            true
        }
    }
}