package com.github.libretube.ui.preferences

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.ui.base.BasePreferenceFragment

class PlayerSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.player

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.player_settings, rootKey)

        val defaultSubtitle = findPreference<ListPreference>(PreferenceKeys.DEFAULT_SUBTITLE)
        defaultSubtitle?.let { setupSubtitlePref(it) }

        val captionSettings = findPreference<Preference>(PreferenceKeys.CAPTION_SETTINGS)
        captionSettings?.setOnPreferenceClickListener {
            try {
                val captionSettingsIntent = Intent(Settings.ACTION_CAPTIONING_SETTINGS)
                startActivity(captionSettingsIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
            }
            true
        }

        val behaviorWhenMinimized =
            findPreference<ListPreference>(PreferenceKeys.BEHAVIOR_WHEN_MINIMIZED)!!
        val alternativePipControls =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.ALTERNATIVE_PIP_CONTROLS)

        val pipAvailable = PictureInPictureCompat.isPictureInPictureAvailable(requireContext())
        if (!pipAvailable) {
            with(behaviorWhenMinimized) {
                // remove PiP option entry
                entries = entries.toList().subList(1, 3).toTypedArray()
                entryValues = entryValues.toList().subList(1, 3).toTypedArray()
                if (value !in entryValues) value = entryValues.first().toString()
            }
        }

        alternativePipControls?.isVisible = pipAvailable
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
