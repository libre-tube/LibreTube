package com.github.libretube.ui.preferences

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.github.libretube.util.BackupHelper
import com.github.libretube.util.ImportHelper
import java.time.LocalDate
import java.time.LocalTime

class BackupRestoreSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.backup_restore

    // backup and restore database
    private lateinit var getBackupFile: ActivityResultLauncher<String>
    private lateinit var createBackupFile: ActivityResultLauncher<String>
    private var backupFile = BackupFile()

    /**
     * result listeners for importing and exporting subscriptions
     */
    private lateinit var getSubscriptionsFile: ActivityResultLauncher<String>
    private lateinit var createSubscriptionsFile: ActivityResultLauncher<String>

    /**
     * result listeners for importing and exporting playlists
     */
    private lateinit var getPlaylistsFile: ActivityResultLauncher<String>
    private lateinit var createPlaylistsFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        getSubscriptionsFile =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                ImportHelper(requireActivity()).importSubscriptions(uri)
            }
        createSubscriptionsFile = registerForActivityResult(
            CreateDocument("application/json")
        ) { uri ->
            ImportHelper(requireActivity()).exportSubscriptions(uri)
        }

        getPlaylistsFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            ImportHelper(requireActivity()).importPlaylists(uri)
        }

        createPlaylistsFile = registerForActivityResult(
            CreateDocument("application/json")
        ) { uri ->
            ImportHelper(requireActivity()).exportPlaylists(uri)
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
            BackupHelper(requireContext()).createAdvancedBackup(uri, backupFile)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importSubscriptions = findPreference<Preference>("import_subscriptions")
        importSubscriptions?.setOnPreferenceClickListener {
            getSubscriptionsFile.launch("*/*")
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            createSubscriptionsFile.launch("subscriptions.json")
            true
        }

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            getPlaylistsFile.launch("*/*")
            true
        }

        val exportPlaylists = findPreference<Preference>("export_playlists")
        exportPlaylists?.setOnPreferenceClickListener {
            createPlaylistsFile.launch("playlists.json")
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
