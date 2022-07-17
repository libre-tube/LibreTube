package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity

class PlayerSettings : PreferenceFragmentCompat() {
    val TAG = "PlayerSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.player_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.audio_video))

        val playerOrientation = findPreference<ListPreference>(PreferenceKeys.FULLSCREEN_ORIENTATION)
        val autoRotateToFullscreen = findPreference<SwitchPreferenceCompat>(PreferenceKeys.AUTO_FULLSCREEN)

        // only show the player orientation option if auto fullscreen is disabled
        playerOrientation?.isEnabled != PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_FULLSCREEN,
            false
        )

        autoRotateToFullscreen?.setOnPreferenceChangeListener { _, newValue ->
            playerOrientation?.isEnabled = newValue != true
            true
        }
    }
}
