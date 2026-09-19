package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.ui.base.BasePreferenceFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistorySettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.history_settings, rootKey)

        // clear search history
        val clearHistory = findPreference<Preference>(PreferenceKeys.CLEAR_SEARCH_HISTORY)
        clearHistory?.setOnPreferenceClickListener {
            showClearDialog(R.string.clear_history) {
                Database.searchHistoryDao().deleteAll()
            }
            true
        }
    }

    private fun showClearDialog(title: Int, actionOnConfirm: suspend () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(R.string.irreversible)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                // clear the selected preference preferences
                CoroutineScope(Dispatchers.IO).launch {
                    actionOnConfirm.invoke()
                }
            }
            .show()
    }
}
