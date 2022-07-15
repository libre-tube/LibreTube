package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.dialogs.RequireRestartDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedSettings : PreferenceFragmentCompat() {
    val TAG = "AdvancedSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.advanced))

        val dataSaverMode = findPreference<SwitchPreferenceCompat>("data_saver_mode")
        dataSaverMode?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val resetSettings = findPreference<Preference>("reset_settings")
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences(requireContext())

                // clear login token
                PreferenceHelper.setToken(requireContext(), "")

                val restartDialog = RequireRestartDialog()
                restartDialog.show(childFragmentManager, "RequireRestartDialog")
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .show()
    }
}
