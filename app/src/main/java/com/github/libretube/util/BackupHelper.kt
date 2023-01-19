package com.github.libretube.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.query
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Backup and restore the preferences
 */
class BackupHelper(private val context: Context) {
    /**
     * Write a [BackupFile] containing the database content as well as the preferences
     */
    fun createAdvancedBackup(uri: Uri?, backupFile: BackupFile) {
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    JsonHelper.json.encodeToStream(backupFile, outputStream)
                }
            } catch (e: Exception) {
                Log.e(TAG(), "Error while writing backup: $e")
            }
        }
    }

    /**
     * Restore data from a [BackupFile]
     */
    fun restoreAdvancedBackup(uri: Uri?) {
        val backupFile = uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                JsonHelper.json.decodeFromStream<BackupFile>(inputStream)
            }
        } ?: return

        query {
            Database.watchHistoryDao().insertAll(
                *backupFile.watchHistory.toTypedArray()
            )
            Database.searchHistoryDao().insertAll(
                *backupFile.searchHistory.toTypedArray()
            )
            Database.watchPositionDao().insertAll(
                *backupFile.watchPositions.toTypedArray()
            )
            Database.localSubscriptionDao().insertAll(
                *backupFile.localSubscriptions.toTypedArray()
            )
            Database.customInstanceDao().insertAll(
                *backupFile.customInstances.toTypedArray()
            )
            Database.playlistBookmarkDao().insertAll(
                *backupFile.playlistBookmarks.toTypedArray()
            )

            backupFile.localPlaylists.forEach {
                Database.localPlaylistsDao().createPlaylist(it.playlist)
                val playlistId = Database.localPlaylistsDao().getAll().last().playlist.id
                it.videos.forEach {
                    it.playlistId = playlistId
                    Database.localPlaylistsDao().addPlaylistVideo(it)
                }
            }

            restorePreferences(backupFile.preferences)
        }
    }

    /**
     * Restore the shared preferences from a backup file
     */
    private fun restorePreferences(preferences: List<PreferenceItem>?) {
        if (preferences == null) return
        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            // clear the previous settings
            clear()

            // decide for each preference which type it is and save it to the preferences
            preferences.forEach {
                val value = it.value.booleanOrNull
                    ?: it.value.floatOrNull
                    ?: it.value.longOrNull
                    ?: it.value.intOrNull
                    ?: it.value.contentOrNull
                when (value) {
                    is Boolean -> putBoolean(it.key, value)
                    is Float -> putFloat(it.key, value)
                    is Long -> putLong(it.key, value)
                    is Int -> {
                        when (it.key) {
                            PreferenceKeys.START_FRAGMENT -> putInt(it.key, value)
                            else -> putLong(it.key, value.toLong())
                        }
                    }
                    is String -> putString(it.key, value)
                }
            }
        }
    }
}
