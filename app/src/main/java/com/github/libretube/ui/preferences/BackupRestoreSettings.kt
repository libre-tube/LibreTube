package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
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
    private val importFormatList get() = listOf(
        ImportFormat.NEWPIPE,
        ImportFormat.FREETUBE,
        ImportFormat.YOUTUBECSV
    ).map { getString(it.value) }
    private val exportFormatList get() = listOf(
        ImportFormat.NEWPIPE,
        ImportFormat.FREETUBE
    ).map { getString(it.value) }

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
                ImportHelper.importSubscriptions(requireActivity(), it, importFormat)
            }
        }
    }

    private val createSubscriptionsFile = registerForActivityResult(CreateDocument(JSON)) {
        it?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                ImportHelper.exportSubscriptions(requireActivity(), it, importFormat)
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

    private fun createImportFormatDialog(
        @StringRes titleStringId: Int,
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importSubscriptions = findPreference<Preference>("import_subscriptions")
        importSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(R.string.import_subscriptions_from, importFormatList) {
                importFormat = ImportFormat.values()[it]
                getSubscriptionsFile.launch("*/*")
            }
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(R.string.export_subscriptions_to, exportFormatList) {
                importFormat = ImportFormat.values()[it]
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
