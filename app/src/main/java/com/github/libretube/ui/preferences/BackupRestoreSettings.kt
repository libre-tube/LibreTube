package com.github.libretube.ui.preferences

import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.ImportFormat
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.github.libretube.ui.dialogs.BackupDialog.Companion.BACKUP_DIALOG_REQUEST_KEY
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRestoreSettings : BasePreferenceFragment() {
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.NEWPIPE

    override val titleResourceId: Int = R.string.backup_restore

    // backup and restore database
    private val getBackupFile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.restoreAdvancedBackup(requireContext().applicationContext, uri)
                withContext(Dispatchers.Main) {
                    // could fail if fragment is already closed
                    runCatching {
                        RequireRestartDialog().show(childFragmentManager, this::class.java.name)
                    }
                }
            }
        }
    private val createBackupFile = registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            BackupHelper.createAdvancedBackup(requireContext().applicationContext, uri, backupFile)
        }
    }

    /**
     * result listeners for importing and exporting subscriptions
     */
    private val getSubscriptionsFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            ImportHelper.importSubscriptions(requireContext().applicationContext, uri, importFormat)
        }
    }

    private val createSubscriptionsFile = registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            ImportHelper.exportSubscriptions(requireContext().applicationContext, uri, importFormat)
        }
    }


    // result listeners for importing and exporting playlists
    private val getPlaylistsFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
            for (file in files) {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importPlaylists(requireContext().applicationContext, file, importFormat)
                }
            }
        }

    private val getWatchHistoryFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
            for (file in files) {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importWatchHistory(requireContext().applicationContext, file, importFormat)
                }
            }
        }

    private val createPlaylistsFile = registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                ImportHelper.exportPlaylists(requireContext().applicationContext, uri, importFormat)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importSubscriptions = findPreference<Preference>("import_subscriptions")
        importSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(requireContext(), R.string.import_subscriptions_from, importSubscriptionFormatList) {
                importFormat = it
                getSubscriptionsFile.launch("*/*")
            }
            true
        }

        val exportSubscriptions = findPreference<Preference>("export_subscriptions")
        exportSubscriptions?.setOnPreferenceClickListener {
            createImportFormatDialog(requireContext(), R.string.export_subscriptions_to, exportSubscriptionFormatList) {
                importFormat = it
                createSubscriptionsFile.launch(
                    "${getString(importFormat.value).lowercase()}-subscriptions.${importFormat.fileExtension}"
                )
            }
            true
        }

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(requireContext(), R.string.import_playlists_from, importPlaylistFormatList) {
                importFormat = it
                getPlaylistsFile.launch(arrayOf("*/*"))
            }
            true
        }

        val exportPlaylists = findPreference<Preference>("export_playlists")
        exportPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(requireContext(), R.string.export_playlists_to, exportPlaylistFormatList) {
                importFormat = it
                createPlaylistsFile.launch(
                    "${getString(importFormat.value).lowercase()}-playlists.${importFormat.fileExtension}"
                )
            }
            true
        }

        val importWatchHistory = findPreference<Preference>("import_watch_history")
        importWatchHistory?.setOnPreferenceClickListener {
            createImportFormatDialog(requireContext(), R.string.import_watch_history, importWatchHistoryFormatList) {
                importFormat = it
                getWatchHistoryFile.launch(arrayOf("*/*"))
            }
            true
        }

        childFragmentManager.setFragmentResultListener(
            BACKUP_DIALOG_REQUEST_KEY,
            this
        ) { _, resultBundle ->
            val encodedBackupFile = resultBundle.getString(IntentData.backupFile)!!
            backupFile = Json.decodeFromString(encodedBackupFile)
            val timestamp = TextUtils.getFileSafeTimeStampNow()
            createBackupFile.launch("libretube-backup-${timestamp}.json")
        }
        val advancedBackup = findPreference<Preference>("backup")
        advancedBackup?.setOnPreferenceClickListener {
            BackupDialog().show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch(JSON)
            true
        }
    }

    companion object {
        const val JSON = "application/json"

        /**
         * Mimetype to use to create new files when setting extension manually
         */
        const val FILETYPE_ANY = "application/octet-stream"

        val importSubscriptionFormatList = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV
        )
        val exportSubscriptionFormatList = listOf(
            ImportFormat.NEWPIPE,
            ImportFormat.FREETUBE
        )
        val importPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE,
            ImportFormat.YOUTUBECSV,
            ImportFormat.URLSORIDS
        )
        val exportPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.FREETUBE
        )
        val importWatchHistoryFormatList = listOf(ImportFormat.YOUTUBEJSON)

        fun createImportFormatDialog(
            context: Context,
            @StringRes titleStringId: Int,
            formats: List<ImportFormat>,
            onConfirm: (ImportFormat) -> Unit
        ) {
            var selectedIndex = 0
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(titleStringId))
                .setSingleChoiceItems(formats.map { context.getString(it.value) }.toTypedArray(), selectedIndex) { _, i ->
                    selectedIndex = i
                }
                .setPositiveButton(
                    R.string.okay
                ) { _, _ -> onConfirm(formats[selectedIndex]) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
