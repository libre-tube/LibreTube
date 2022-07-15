package com.github.libretube.preferences

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.BuildConfig
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.dialogs.RequireRestartDialog
import com.github.libretube.util.ThemeHelper
import com.github.libretube.util.checkUpdate

class MainSettings : PreferenceFragmentCompat() {
    val TAG = "SettingsFragment"

    companion object {
        lateinit var getContent: ActivityResultLauncher<String>
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val region = findPreference<Preference>("region")
        region?.setOnPreferenceChangeListener { _, _ ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val language = findPreference<ListPreference>("language")
        language?.setOnPreferenceChangeListener { _, _ ->
            ThemeHelper.restartMainActivity(requireContext())
            true
        }

        val instance = findPreference<Preference>("instance")
        instance?.setOnPreferenceClickListener {
            val newFragment = InstanceSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val appearance = findPreference<Preference>("appearance")
        appearance?.setOnPreferenceClickListener {
            val newFragment = AppearanceSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val sponsorBlock = findPreference<Preference>("sponsorblock")
        sponsorBlock?.setOnPreferenceClickListener {
            val newFragment = SponsorBlockSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val player = findPreference<Preference>("player")
        player?.setOnPreferenceClickListener {
            val newFragment = PlayerSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val history = findPreference<Preference>("history")
        history?.setOnPreferenceClickListener {
            val newFragment = HistorySettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val advanced = findPreference<Preference>("advanced")
        advanced?.setOnPreferenceClickListener {
            val newFragment = AdvancedSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val update = findPreference<Preference>("update")
        update?.title = getString(R.string.version, BuildConfig.VERSION_NAME)
        update?.setOnPreferenceClickListener {
            checkUpdate(childFragmentManager)
            true
        }

        val about = findPreference<Preference>("about")
        about?.setOnPreferenceClickListener {
            val newFragment = AboutFragment()
            navigateToSettingsFragment(newFragment)
            true
        }
    }

    private fun navigateToSettingsFragment(newFragment: Fragment) {
        Globals.isCurrentViewMainSettings = false
        parentFragmentManager.beginTransaction()
            .replace(R.id.settings, newFragment)
            .commitNow()
    }
}
