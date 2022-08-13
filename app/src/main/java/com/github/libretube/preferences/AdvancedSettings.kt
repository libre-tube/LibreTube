package com.github.libretube.preferences

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.util.BackupHelper
import com.github.libretube.views.MaterialPreferenceFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdvancedSettings : MaterialPreferenceFragment() {
    val TAG = "AdvancedSettings"

    /**
     * result listeners for importing and exporting subscriptions
     */
    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var createFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        getContent =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                BackupHelper(requireContext()).restoreSharedPreferences(uri)
            }
        createFile = registerForActivityResult(
            ActivityResultContracts.CreateDocument()
        ) { uri: Uri? ->
            BackupHelper(requireContext()).backupSharedPreferences(uri)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.advanced))

        val resetSettings = findPreference<Preference>(PreferenceKeys.RESET_SETTINGS)
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }

        val backupSettings = findPreference<Preference>(PreferenceKeys.BACKUP_SETTINGS)
        backupSettings?.setOnPreferenceClickListener {
            createFile.launch("preferences.xml")
            true
        }

        val restoreSettings = findPreference<Preference>(PreferenceKeys.RESTORE_SETTINGS)
        restoreSettings?.setOnPreferenceClickListener {
            getContent.launch("*/*")
            // reset the token
            PreferenceHelper.setToken("")
            activity?.recreate()
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
