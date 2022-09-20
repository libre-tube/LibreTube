package com.github.libretube.ui.preferences

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.views.MaterialPreferenceFragment

class SponsorBlockSettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.sponsorblock))
    }
}
