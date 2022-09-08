package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.views.MaterialPreferenceFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistorySettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.history_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.history))

        // clear search history
        val clearHistory = findPreference<Preference>(PreferenceKeys.CLEAR_SEARCH_HISTORY)
        clearHistory?.setOnPreferenceClickListener {
            showClearDialog(R.string.clear_history) {
                DatabaseHolder.db.searchHistoryDao().deleteAll()
            }
            true
        }

        // clear watch history and positions
        val clearWatchHistory = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_HISTORY)
        clearWatchHistory?.setOnPreferenceClickListener {
            showClearDialog(R.string.clear_history) {
                DatabaseHolder.db.watchHistoryDao().deleteAll()
            }
            true
        }

        // clear watch positions
        val clearWatchPositions = findPreference<Preference>(PreferenceKeys.CLEAR_WATCH_POSITIONS)
        clearWatchPositions?.setOnPreferenceClickListener {
            showClearDialog(R.string.reset_watch_positions) {
                DatabaseHolder.db.watchPositionDao().deleteAll()
            }
            true
        }
    }

    private fun showClearDialog(title: Int, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(R.string.irreversible)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                // clear the selected preference preferences
                Thread {
                    action()
                }.start()
            }
            .show()
    }
}
