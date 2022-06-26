package com.github.libretube.preferences

import android.os.Bundle
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.requireMainActivityRestart
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedSettings : PreferenceFragmentCompat() {
    val TAG = "AdvancedSettings"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_settings, rootKey)

        val topBarTextView = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarTextView?.text = getString(R.string.advanced)

        val clearHistory = findPreference<Preference>("clear_history")
        clearHistory?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference(requireContext(), "search_history")
            true
        }

        val resetSettings = findPreference<Preference>("reset_settings")
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences(requireContext())

                // clear login token
                PreferenceHelper.setToken(requireContext(), "")

                requireMainActivityRestart = true
                activity?.recreate()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .show()
    }
}
