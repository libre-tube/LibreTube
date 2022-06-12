package com.github.libretube.preferences

import android.os.Bundle
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.requireMainActivityRestart
import com.github.libretube.util.ThemeHelper

class AppearanceSettings : PreferenceFragmentCompat() {
    private val TAG = "AppearanceSettings"
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_settings, rootKey)

        val topBarTextView = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarTextView?.text = getString(R.string.appearance)

        val themeToggle = findPreference<ListPreference>("theme_togglee")
        themeToggle?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivityRestart = true
            ThemeHelper().restartMainActivity(requireContext())
            true
        }

        val accentColor = findPreference<Preference>("accent_color")
        accentColor?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivityRestart = true
            activity?.recreate()
            true
        }

        val iconChange = findPreference<ListPreference>("icon_change")
        iconChange?.setOnPreferenceChangeListener { _, newValue ->
            ThemeHelper().changeIcon(requireContext(), newValue.toString())
            true
        }

        val gridColumns = findPreference<ListPreference>("grid")
        gridColumns?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivityRestart = true
            true
        }
    }
}
