package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
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
    override val titleResourceId: Int = R.string.settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val general = findPreference<Preference>("general")
        general?.setOnPreferenceClickListener {
            navigateToSettingsFragment(GeneralSettings())
        }

        val instance = findPreference<Preference>("instance")
        instance?.setOnPreferenceClickListener {
            navigateToSettingsFragment(InstanceSettings())
        }

        val appearance = findPreference<Preference>("appearance")
        appearance?.setOnPreferenceClickListener {
            navigateToSettingsFragment(AppearanceSettings())
        }

        val sponsorBlock = findPreference<Preference>("sponsorblock")
        sponsorBlock?.setOnPreferenceClickListener {
            navigateToSettingsFragment(SponsorBlockSettings())
        }

        val player = findPreference<Preference>("player")
        player?.setOnPreferenceClickListener {
            navigateToSettingsFragment(PlayerSettings())
        }

        val audioVideo = findPreference<Preference>("audio_video")
        audioVideo?.setOnPreferenceClickListener {
            navigateToSettingsFragment(AudioVideoSettings())
        }

        val history = findPreference<Preference>("history")
        history?.setOnPreferenceClickListener {
            navigateToSettingsFragment(HistorySettings())
        }

        val notifications = findPreference<Preference>("notifications")
        notifications?.setOnPreferenceClickListener {
            navigateToSettingsFragment(NotificationSettings())
        }

        val backupRestore = findPreference<Preference>("backup_restore")
        backupRestore?.setOnPreferenceClickListener {
            navigateToSettingsFragment(BackupRestoreSettings())
        }

        val advanced = findPreference<Preference>("advanced")
        advanced?.setOnPreferenceClickListener {
            navigateToSettingsFragment(AdvancedSettings())
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
            if (BuildConfig.DEBUG) {
                Toast.makeText(
                    context,
                    "Updater is disabled for debug versions!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceClickListener true
            }
            CoroutineScope(Dispatchers.IO).launch {
                // check for update
                val updateInfo = try {
                    RetrofitInstance.externalApi.getUpdateInfo()
                } catch (e: Exception) {
                    showSnackBar(R.string.unknown_error)
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

    private fun navigateToSettingsFragment(newFragment: Fragment): Boolean {
        parentFragmentManager.commitNow {
            replace(R.id.settings, newFragment)
        }
        return true
    }
}
