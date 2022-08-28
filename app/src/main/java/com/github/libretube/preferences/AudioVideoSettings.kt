package com.github.libretube.preferences

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.views.MaterialPreferenceFragment
import java.util.*

class AudioVideoSettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.audio_video_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.audio_video))
    }
}
