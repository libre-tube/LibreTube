package com.github.libretube.preferences

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.dialogs.BackupDialog
import com.github.libretube.util.BackupHelper
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.views.MaterialPreferenceFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedSettings : MaterialPreferenceFragment() {

    // backup and restore prefs
    private lateinit var getPrefFile: ActivityResultLauncher<String>
    private lateinit var createPrefFile: ActivityResultLauncher<String>

    // backup and restore database
    private lateinit var getBackupFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        getPrefFile =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                BackupHelper(requireContext()).restoreSharedPreferences(uri)
            }
        createPrefFile = registerForActivityResult(
            CreateDocument("application/json")
        ) { uri: Uri? ->
            BackupHelper(requireContext()).backupSharedPreferences(uri)
        }

        getBackupFile =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                BackupHelper(requireContext()).restoreAdvancedBackup(uri)
            }
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.advanced))

        val maxImageCache = findPreference<ListPreference>(PreferenceKeys.MAX_IMAGE_CACHE)
        maxImageCache?.setOnPreferenceChangeListener { _, _ ->
            ImageHelper.initializeImageLoader(requireContext())
            true
        }

        val resetSettings = findPreference<Preference>(PreferenceKeys.RESET_SETTINGS)
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }

        val backupSettings = findPreference<Preference>(PreferenceKeys.BACKUP_SETTINGS)
        backupSettings?.setOnPreferenceClickListener {
            createPrefFile.launch("preferences.xml")
            true
        }

        val restoreSettings = findPreference<Preference>(PreferenceKeys.RESTORE_SETTINGS)
        restoreSettings?.setOnPreferenceClickListener {
            getPrefFile.launch("*/*")
            // reset the token
            PreferenceHelper.setToken("")
            activity?.recreate()
            true
        }

        val advancesBackup = findPreference<Preference>("backup")
        advancesBackup?.setOnPreferenceClickListener {
            BackupDialog()
                .show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch("application/json")
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences()

                // clear login token
                PreferenceHelper.setToken("")

                activity?.recreate()
            }
            .show()
    }
}
