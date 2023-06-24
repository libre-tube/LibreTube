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
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupRestoreSettings : BasePreferenceFragment() {
    private val backupDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss")
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.NEWPIPE
    private val importSubscriptionFormatList
        get() = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV
        )
    private val exportSubscriptionFormatList
        get() = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE
        )
    private val importPlaylistFormatList
        get() = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV
        )
    private val exportPlaylistFormatList
        get() = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE
        )

    override val titleResourceId: Int = R.string.backup_restore

    // backup and restore database
    private val getBackupFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.restoreAdvancedBackup(requireContext(), it)
                withContext(Dispatchers.Main) {
                    // could fail if fragment is already closed
                    runCatching {
                        RequireRestartDialog().show(childFragmentManager, this::class.java.name)
                    }
                }
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
        ActivityResultContracts.GetContent()
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
                    ImportHelper.importPlaylists(requireActivity(), it, importFormat)
                }
            }
        }
    private val createPlaylistsFile = registerForActivityResult(CreateDocument(JSON)) {
        it?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                ImportHelper.exportPlaylists(requireActivity(), it, importFormat)
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
            val list = importSubscriptionFormatList.map { getString(it.value) }
            createImportFormatDialog(R.string.import_subscriptions_from, list) {
                importFormat = importSubscriptionFormatList[it]
                getSubscriptionsFile.launch("*/*")
            }
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            val list = exportSubscriptionFormatList.map { getString(it.value) }
            createImportFormatDialog(R.string.export_subscriptions_to, list) {
                importFormat = exportSubscriptionFormatList[it]
                createSubscriptionsFile.launch(
                    "${getString(importFormat.value).lowercase()}-subscriptions.json"
                )
            }
            true
        }

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            val list = importPlaylistFormatList.map { getString(it.value) }
            createImportFormatDialog(R.string.import_playlists_from, list) {
                importFormat = importPlaylistFormatList[it]
                getPlaylistsFile.launch(arrayOf("*/*"))
            }
            true
        }

        val exportPlaylists = findPreference<Preference>("export_playlists")
        exportPlaylists?.setOnPreferenceClickListener {
            val list = exportPlaylistFormatList.map { getString(it.value) }
            createImportFormatDialog(R.string.export_playlists_to, list) {
                importFormat = exportPlaylistFormatList[it]
                createPlaylistsFile.launch(
                    "${getString(importFormat.value).lowercase()}-playlists.json"
                )
            }
            true
        }

        val advancedBackup = findPreference<Preference>("backup")
        advancedBackup?.setOnPreferenceClickListener {
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
