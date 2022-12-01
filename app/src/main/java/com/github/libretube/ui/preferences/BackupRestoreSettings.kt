package com.github.libretube.ui.preferences

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.github.libretube.util.BackupHelper
import com.github.libretube.util.ImportHelper
import java.time.LocalDate
import java.time.LocalTime

class BackupRestoreSettings : BasePreferenceFragment() {

    // backup and restore database
    private lateinit var getBackupFile: ActivityResultLauncher<String>
    private lateinit var createBackupFile: ActivityResultLauncher<String>
    private var backupFile = BackupFile()

    /**
     * result listeners for importing and exporting subscriptions
     */
    private lateinit var getSubscriptionsFile: ActivityResultLauncher<String>
    private lateinit var createSubscriptionsFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        getSubscriptionsFile =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                ImportHelper(requireActivity()).importSubscriptions(uri)
            }
        createSubscriptionsFile = registerForActivityResult(
            CreateDocument("application/json")
        ) { uri: Uri? ->
            ImportHelper(requireActivity()).exportSubscriptions(uri)
        }

        getBackupFile =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                BackupHelper(requireContext()).restoreAdvancedBackup(uri)
            }

        createBackupFile = registerForActivityResult(
            CreateDocument("application/json")
        ) { uri: Uri? ->
            BackupHelper(requireContext()).advancedBackup(uri, backupFile)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importSubscriptions = findPreference<Preference>(PreferenceKeys.IMPORT_SUBS)
        importSubscriptions?.setOnPreferenceClickListener {
            // check StorageAccess
            getSubscriptionsFile.launch("*/*")
            true
        }

        val exportSubscriptions = findPreference<Preference>(PreferenceKeys.EXPORT_SUBS)
        exportSubscriptions?.setOnPreferenceClickListener {
            createSubscriptionsFile.launch("subscriptions.json")
            true
        }

        val advancesBackup = findPreference<Preference>("backup")
        advancesBackup?.setOnPreferenceClickListener {
            BackupDialog {
                backupFile = it
                createBackupFile.launch(getBackupFileName())
            }
                .show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch("application/json")
            true
        }
    }

    private fun getBackupFileName(): String {
        val time = LocalTime.now().toString().split(".").firstOrNull()
        return "libretube-backup-${LocalDate.now()}-$time.json"
    }
}
