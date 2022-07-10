package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.dialogs.RequireRestartDialog
import com.github.libretube.util.ThemeHelper
import com.google.android.material.color.DynamicColors

class AppearanceSettings : PreferenceFragmentCompat() {
    private val TAG = "AppearanceSettings"
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.appearance))

        val themeToggle = findPreference<ListPreference>("theme_togglee")
        themeToggle?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val accentColor = findPreference<ListPreference>("accent_color")
        updateAccentColorValues(accentColor!!)
        accentColor.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val iconChange = findPreference<ListPreference>("icon_change")
        iconChange?.setOnPreferenceChangeListener { _, newValue ->
            ThemeHelper.changeIcon(requireContext(), newValue.toString())
            true
        }

        val gridColumns = findPreference<ListPreference>("grid")
        gridColumns?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val hideTrending = findPreference<SwitchPreference>("hide_trending_page")
        hideTrending?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val labelVisibilityMode = findPreference<ListPreference>("label_visibility")
        labelVisibilityMode?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }
    }

    // remove material you from accent color option if not available
    private fun updateAccentColorValues(pref: ListPreference) {
        val dynamicColorsAvailable = DynamicColors.isDynamicColorAvailable()
        if (!dynamicColorsAvailable) {
            val entries = pref.entries.toMutableList()
            entries -= entries[0]
            pref.entries = entries.toTypedArray()
            val values = pref.entryValues.toMutableList()
            values -= values[0]
            pref.entryValues = values.toTypedArray()
        }
    }
}
