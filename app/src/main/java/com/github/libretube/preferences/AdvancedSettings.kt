package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.views.MaterialPreferenceFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedSettings : MaterialPreferenceFragment() {
    val TAG = "AdvancedSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.advanced))

        val resetSettings = findPreference<Preference>(PreferenceKeys.RESET_SETTINGS)
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences()

                // clear login token
                PreferenceHelper.setToken("")

                activity?.recreate()
            }
            .show()
    }
}
