package com.github.libretube.ui.preferences

import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.RequireRestartDialog

class GeneralSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.general

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        val language = findPreference<ListPreference>("language")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            language?.setOnPreferenceChangeListener { _, _ ->
                RequireRestartDialog().show(
                    childFragmentManager,
                    RequireRestartDialog::class.java.name
                )
                true
            }
            val languages = requireContext().resources.getStringArray(R.array.languageCodes)
                .map { code ->
                    val locale = LocaleHelper.getLocaleFromAndroidCode(code)

                    // each language's name is displayed in its own language,
                    // e.g. 'de': 'Deutsch', 'fr': 'Francais', ...
                    locale.toString() to locale.getDisplayName(locale)
                }.sortedBy { it.second.lowercase() }
            language?.entries =
                arrayOf(requireContext().getString(R.string.systemLanguage)) + languages.map { it.second }
            language?.entryValues = arrayOf("sys") + languages.map { it.first }
        } else {
            // language is set through Android settings
            language?.isVisible = false
        }

        val region = findPreference<ListPreference>("region")
        region?.let { setupRegionPref(it) }

        val autoRotation = findPreference<ListPreference>(PreferenceKeys.ORIENTATION)
        autoRotation?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
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
