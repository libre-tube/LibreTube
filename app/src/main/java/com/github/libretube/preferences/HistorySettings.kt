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
        val clearHistory = findPreference<Preference>("clear_history")
        clearHistory?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference(requireContext(), "search_history")
            true
        }

        // clear watch history and positions
        val clearWatchHistory = findPreference<Preference>("clear_watch_history")
        clearWatchHistory?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference(requireContext(), "watch_history")
            true
        }

        // clear watch positions
        val clearWatchPositions = findPreference<Preference>("clear_watch_positions")
        clearWatchPositions?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference(requireContext(), "watch_positions")
            true
        }
    }
}
