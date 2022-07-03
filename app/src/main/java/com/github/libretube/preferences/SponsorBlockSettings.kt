package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity

class SponsorBlockSettings : PreferenceFragmentCompat() {
    private val TAG = "SponsorBlockSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.sponsorblock))
    }
}
