package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity

class PlayerSettings : PreferenceFragmentCompat() {
    val TAG = "PlayerSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.player_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.player))
    }
}
