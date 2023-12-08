package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class BackupDialog : DialogFragment() {
    sealed class BackupOption(
        @StringRes val name: Int,
        val onSelected: suspend (BackupFile) -> Unit
    ) {
        data object WatchHistory : BackupOption(R.string.watch_history, onSelected = {
            it.watchHistory = Database.watchHistoryDao().getAll()
        })

        data object WatchPositions : BackupOption(R.string.watch_positions, onSelected = {
            it.watchPositions = Database.watchPositionDao().getAll()
        })

        data object SearchHistory : BackupOption(R.string.search_history, onSelected = {
            it.searchHistory = Database.searchHistoryDao().getAll()
        })

        data object LocalSubscriptions : BackupOption(R.string.local_subscriptions, onSelected = {
            it.localSubscriptions = Database.localSubscriptionDao().getAll()
        })

        data object CustomInstances : BackupOption(R.string.backup_customInstances, onSelected = {
            it.customInstances = Database.customInstanceDao().getAll()
        })

        data object PlaylistBookmarks : BackupOption(R.string.bookmarks, onSelected = {
            it.playlistBookmarks = Database.playlistBookmarkDao().getAll()
        })

        data object LocalPlaylists : BackupOption(R.string.local_playlists, onSelected = {
            it.localPlaylists = Database.localPlaylistsDao().getAll()
        })

        data object SubscriptionGroups : BackupOption(R.string.channel_groups, onSelected = {
            it.channelGroups = Database.subscriptionGroupsDao().getAll()
        })

        data object Preferences : BackupOption(R.string.preferences, onSelected = { file ->
            file.preferences = PreferenceHelper.settings.all.map { (key, value) ->
                val jsonValue = when (value) {
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is String -> JsonPrimitive(value)
                    is Set<*> -> JsonPrimitive(value.joinToString(","))
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
            BackupOption.SubscriptionGroups,
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
            .setPositiveButton(R.string.backup, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch(Dispatchers.IO) {
                        val backupFile = BackupFile()

                        backupOptions.forEachIndexed { index, option ->
                            if (selected[index]) option.onSelected(backupFile)
                        }

                        val encodedBackupFile = Json.encodeToString(backupFile)
                        setFragmentResult(
                            BACKUP_DIALOG_REQUEST_KEY,
                            bundleOf(IntentData.backupFile to encodedBackupFile)
                        )

                        dialog?.dismiss()
                    }
                }
            }
    }

    companion object {
        const val BACKUP_DIALOG_REQUEST_KEY = "backup_dialog_request_key"
    }
}
