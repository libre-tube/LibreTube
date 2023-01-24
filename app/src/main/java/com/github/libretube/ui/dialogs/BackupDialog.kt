package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class BackupDialog(
    private val createBackupFile: (BackupFile) -> Unit
) : DialogFragment() {
    sealed class BackupOption(
        @StringRes val name: Int,
        val onSelected: suspend (BackupFile) -> Unit
    ) {
        object WatchHistory : BackupOption(R.string.watch_history, onSelected = {
            it.watchHistory = Database.watchHistoryDao().getAll()
        })

        object WatchPositions : BackupOption(R.string.watch_positions, onSelected = {
            it.watchPositions = Database.watchPositionDao().getAll()
        })

        object SearchHistory : BackupOption(R.string.search_history, onSelected = {
            it.searchHistory = Database.searchHistoryDao().getAll()
        })

        object LocalSubscriptions : BackupOption(R.string.local_subscriptions, onSelected = {
            it.localSubscriptions = Database.localSubscriptionDao().getAll()
        })

        object CustomInstances : BackupOption(R.string.backup_customInstances, onSelected = {
            it.customInstances = Database.customInstanceDao().getAll()
        })

        object PlaylistBookmarks : BackupOption(R.string.bookmarks, onSelected = {
            it.playlistBookmarks = Database.playlistBookmarkDao().getAll()
        })

        object LocalPlaylists : BackupOption(R.string.local_playlists, onSelected = {
            it.localPlaylists = Database.localPlaylistsDao().getAll()
        })

        object Preferences : BackupOption(R.string.preferences, onSelected = { file ->
            file.preferences = PreferenceHelper.settings.all.map { (key, value) ->
                val jsonValue = when (value) {
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is String -> JsonPrimitive(value)
                    else -> JsonNull
                }
                PreferenceItem(key, jsonValue)
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val backupOptions = listOf(
            BackupOption.WatchHistory,
            BackupOption.WatchPositions,
            BackupOption.SearchHistory,
            BackupOption.LocalSubscriptions,
            BackupOption.CustomInstances,
            BackupOption.PlaylistBookmarks,
            BackupOption.LocalPlaylists,
            BackupOption.Preferences
        )

        val backupItems = backupOptions.map { context?.getString(it.name)!! }.toTypedArray()

        val selected = BooleanArray(backupOptions.size) { true }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup)
            .setMultiChoiceItems(backupItems, selected) { _, index, newValue ->
                selected[index] = newValue
            }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup) { _, _ ->
                val backupFile = BackupFile()
                lifecycleScope.launch(Dispatchers.IO) {
                    backupOptions.forEachIndexed { index, option ->
                        if (selected[index]) option.onSelected(backupFile)
                    }
                }
                createBackupFile(backupFile)
            }
            .create()
    }
}
