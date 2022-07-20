package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
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

        val themeToggle = findPreference<ListPreference>(PreferenceKeys.THEME_MODE)
        themeToggle?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val pureTheme = findPreference<SwitchPreferenceCompat>(PreferenceKeys.PURE_THEME)
        pureTheme?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val accentColor = findPreference<ListPreference>(PreferenceKeys.ACCENT_COLOR)
        updateAccentColorValues(accentColor!!)
        accentColor.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val iconChange = findPreference<ListPreference>(PreferenceKeys.APP_ICON)
        iconChange?.setOnPreferenceChangeListener { _, newValue ->
            ThemeHelper.changeIcon(requireContext(), newValue.toString())
            true
        }

        val gridColumns = findPreference<ListPreference>(PreferenceKeys.GRID_COLUMNS)
        gridColumns?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val labelVisibilityMode = findPreference<ListPreference>(PreferenceKeys.LABEL_VISIBILITY)
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
