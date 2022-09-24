package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.NavBarOptionsDialog
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.PreferenceHelper

class GeneralSettings : BasePreferenceFragment() {

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

        val region = findPreference<ListPreference>("region")
        region?.let { setupRegionPref(it) }

        val autoRotation = findPreference<SwitchPreferenceCompat>(PreferenceKeys.AUTO_ROTATION)
        autoRotation?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val navBarOptions = findPreference<Preference>(PreferenceKeys.NAVBAR_ITEMS)
        navBarOptions?.setOnPreferenceClickListener {
            NavBarOptionsDialog().show(
                childFragmentManager,
                null
            )
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

    private fun setupRegionPref(preference: ListPreference) {
        val countries = LocaleHelper.getAvailableCountries()
        val countryNames = countries.map { it.name }
            .toMutableList()
        countryNames.add(0, requireContext().getString(R.string.systemLanguage))

        val countryCodes = countries.map { it.code }
            .toMutableList()
        countryCodes.add(0, "sys")

        preference.entries = countryNames.toTypedArray()
        preference.entryValues = countryCodes.toTypedArray()
        preference.summaryProvider = Preference.SummaryProvider<ListPreference> {
            it.entry
        }
    }
}
