package com.github.libretube.ui.preferences

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.ui.base.BasePreferenceFragment

class SponsorBlockSettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)
    }
}
