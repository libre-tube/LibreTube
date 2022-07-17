package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity

class HistorySettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.history_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.history))

        // clear search history
        val clearHistory = findPreference<Preference>(PreferenceKeys.CLEAR_SEARCH_HISTORY)
        clearHistory?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference("search_history")
            true
        }

        // clear watch history and positions
        val clearWatchHistory = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_HISTORY)
        clearWatchHistory?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference("watch_history")
            true
        }

        // clear watch positions
        val clearWatchPositions = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_POSITIONS)
        clearWatchPositions?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference("watch_positions")
            true
        }
    }
}
