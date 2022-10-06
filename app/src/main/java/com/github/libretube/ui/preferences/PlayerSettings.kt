package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.PreferenceHelper

class PlayerSettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.player_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.player))

        val playerOrientation =
            findPreference<ListPreference>(PreferenceKeys.FULLSCREEN_ORIENTATION)
        val autoRotateToFullscreen =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.AUTO_FULLSCREEN)

        // only show the player orientation option if auto fullscreen is disabled
        playerOrientation?.isEnabled = !PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_FULLSCREEN,
            false
        )

        autoRotateToFullscreen?.setOnPreferenceChangeListener { _, newValue ->
            playerOrientation?.isEnabled = newValue != true
            true
        }

        val defaultSubtitle = findPreference<ListPreference>(PreferenceKeys.DEFAULT_SUBTITLE)
        defaultSubtitle?.let { setupSubtitlePref(it) }
    }

    private fun setupSubtitlePref(preference: ListPreference) {
        val locales = LocaleHelper.getAvailableLocales()
        val localeNames = locales.map { it.name }
            .toMutableList()
        localeNames.add(0, requireContext().getString(R.string.none))

        val localeCodes = locales.map { it.code }
            .toMutableList()
        localeCodes.add(0, "")

        preference.entries = localeNames.toTypedArray()
        preference.entryValues = localeCodes.toTypedArray()
        preference.summaryProvider =
            Preference.SummaryProvider<ListPreference> {
                it.entry
            }
    }
}
