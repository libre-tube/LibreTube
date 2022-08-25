package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.dialogs.RequireRestartDialog
import com.github.libretube.views.MaterialPreferenceFragment

class GeneralSettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.general))

        val language = findPreference<ListPreference>("language")
        language?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val autoRotation = findPreference<SwitchPreferenceCompat>(PreferenceKeys.AUTO_ROTATION)
        autoRotation?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val hideTrending = findPreference<SwitchPreferenceCompat>(PreferenceKeys.HIDE_TRENDING_PAGE)
        hideTrending?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val breakReminder =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.BREAK_REMINDER_TOGGLE)
        val breakReminderTime = findPreference<EditTextPreference>(PreferenceKeys.BREAK_REMINDER)
        breakReminderTime?.isEnabled = PreferenceHelper.getBoolean(
            PreferenceKeys.BREAK_REMINDER_TOGGLE,
            false
        )

        breakReminder?.setOnPreferenceChangeListener { _, newValue ->
            breakReminderTime?.isEnabled = newValue as Boolean
            true
        }
    }
}
