package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistorySettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.history_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.history))

        // clear search history
        val clearHistory = findPreference<Preference>(PreferenceKeys.CLEAR_SEARCH_HISTORY)
        clearHistory?.setOnPreferenceClickListener {
            showClearDialog(R.string.clear_history, "search_history")
            true
        }

        // clear watch history and positions
        val clearWatchHistory = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_HISTORY)
        clearWatchHistory?.setOnPreferenceClickListener {
            showClearDialog(R.string.clear_history, "watch_history")
            true
        }

        // clear watch positions
        val clearWatchPositions = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_POSITIONS)
        clearWatchPositions?.setOnPreferenceClickListener {
            showClearDialog(R.string.reset_watch_positions, "watch_positions")
            true
        }
    }

    private fun showClearDialog(title: Int, preferenceKey: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(R.string.irreversible)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                // clear the selected preference preferences
                PreferenceHelper.removePreference(preferenceKey)
            }
            .show()
    }
}
