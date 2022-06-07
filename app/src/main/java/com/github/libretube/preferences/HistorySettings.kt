package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.github.libretube.R

class HistorySettings : PreferenceFragmentCompat() {
    private val TAG = "HistorySettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.history_settings, rootKey)

        val clearHistory = findPreference<Preference>("clear_history")
        clearHistory?.setOnPreferenceClickListener {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            sharedPreferences.edit().remove("search_history").commit()
            true
        }
    }
}
