package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.UpdateDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainSettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val general = findPreference<Preference>("general")
        general?.setOnPreferenceClickListener {
            val newFragment = GeneralSettings()
            navigateToSettingsFragment(newFragment)
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

        val audioVideo = findPreference<Preference>("audio_video")
        audioVideo?.setOnPreferenceClickListener {
            val newFragment = AudioVideoSettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val history = findPreference<Preference>("history")
        history?.setOnPreferenceClickListener {
            val newFragment = HistorySettings()
            navigateToSettingsFragment(newFragment)
            true
        }

        val notifications = findPreference<Preference>("notifications")
        notifications?.setOnPreferenceClickListener {
            val newFragment = NotificationSettings()
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

        // set the version of the update preference
        val versionString = if (BuildConfig.DEBUG) {
            "${BuildConfig.VERSION_NAME} Debug"
        } else {
            getString(R.string.version, BuildConfig.VERSION_NAME)
        }
        update?.title = versionString

        // checking for update: yes -> dialog, no -> snackBar
        update?.setOnPreferenceClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                // check for update
                val updateInfo = try {
                    RetrofitInstance.externalApi.getUpdateInfo()
                } catch (e: Exception) {
                    showSnackBar(R.string.server_error)
                    return@launch
                }

                if (BuildConfig.VERSION_NAME != updateInfo.name) {
                    // show the UpdateAvailableDialog if there's an update available
                    UpdateDialog(updateInfo).show(
                        childFragmentManager,
                        UpdateDialog::class.java.name
                    )
                } else {
                    // otherwise show the no update available snackBar
                    showSnackBar(R.string.app_uptodate)
                }
            }
            true
        }
    }

    private fun showSnackBar(@StringRes text: Int) {
        (activity as? SettingsActivity)?.binding?.let {
            Snackbar.make(it.root, text, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun navigateToSettingsFragment(newFragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.settings, newFragment)
            .commitNow()
    }
}
