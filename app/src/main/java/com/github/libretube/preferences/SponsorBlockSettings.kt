package com.github.libretube.preferences

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.views.MaterialPreferenceFragment

class SponsorBlockSettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.sponsorblock))
    }
}
