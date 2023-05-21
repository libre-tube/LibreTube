package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.enums.ImportFormat
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupRestoreSettings : BasePreferenceFragment() {
    private val backupDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss")
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.NEWPIPE

    override val titleResourceId: Int = R.string.backup_restore

    // backup and restore database
    private val getBackupFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.restoreAdvancedBackup(requireContext(), it)
            }
        }
    }
    private val createBackupFile = registerForActivityResult(CreateDocument(JSON)) {
        it?.let {
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.createAdvancedBackup(requireContext(), it, backupFile)
            }
        }
    }

    /**
     * result listeners for importing and exporting subscriptions
     */
    private val getSubscriptionsFile = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) {
        it?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                importFormat.let { format ->
                    ImportHelper.importSubscriptions(requireActivity(), it, format)
                }
            }
        }
    }
    private val createSubscriptionsFile = registerForActivityResult(CreateDocument(JSON)) {
        it?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                importFormat.let { format ->
                    ImportHelper.exportSubscriptions(requireActivity(), it, format)
                }
            }
        }
    }

    /**
     * result listeners for importing and exporting playlists
     */
    private val getPlaylistsFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            it?.forEach {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importPlaylists(requireActivity(), it)
                }
            }
        }
    private val createPlaylistsFile = registerForActivityResult(CreateDocument(JSON)) {
        it?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                ImportHelper.exportPlaylists(requireActivity(), it)
            }
        }
    }

    private fun createDialog(
        titleStringId: Int,
        items: List<String>,
        onConfirm: (Int) -> Unit
    ) {
        var selectedIndex = 0
        MaterialAlertDialogBuilder(this.requireContext())
            .setTitle(getString(titleStringId))
            .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { _, i ->
                selectedIndex = i
            }
            .setPositiveButton(
                R.string.okay
            ) { _, _ -> onConfirm(selectedIndex) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importFormatList(): List<String> {
        return ImportFormat.values().map {
            when (it) {
                ImportFormat.NEWPIPE -> getString(R.string.import_format_newpipe)
                ImportFormat.FREETUBE -> getString(R.string.import_format_freetube)
                ImportFormat.YOUTUBECSV -> getString(R.string.import_format_youtube_csv)
            }
        }
    }

    private fun exportFormatList(): List<String> {
        return listOf(getString(R.string.import_format_newpipe), getString(R.string.import_format_freetube))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importSubscriptions = findPreference<Preference>("import_subscriptions")
        importSubscriptions?.setOnPreferenceClickListener {
            createDialog(R.string.import_subscriptions_from, importFormatList()) {
                importFormat = ImportFormat.fromInt(it)
                getSubscriptionsFile.launch("*/*")
            }
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            createDialog(R.string.export_subscriptions_to, exportFormatList()) {
                importFormat = ImportFormat.fromInt(it)
                createSubscriptionsFile.launch("subscriptions.json")
            }
            true
        }

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            getPlaylistsFile.launch(arrayOf("*/*"))
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
                val timestamp = backupDateTimeFormatter.format(LocalDateTime.now())
                createBackupFile.launch("libretube-backup-$timestamp.json")
            }
                .show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch(JSON)
            true
        }
    }

    companion object {
        private const val JSON = "application/json"
    }
}
