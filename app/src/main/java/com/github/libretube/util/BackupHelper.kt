package com.github.libretube.util

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.query
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import java.io.FileOutputStream

/**
 * Backup and restore the preferences
 */
class BackupHelper(private val context: Context) {
    /**
     * Write a [BackupFile] containing the database content as well as the preferences
     */
    fun advancedBackup(uri: Uri?, backupFile: BackupFile) {
        if (uri == null) return
        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                    fileOutputStream.write(
                        ObjectMapper().writeValueAsBytes(backupFile)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore data from a [BackupFile]
     */
    fun restoreAdvancedBackup(uri: Uri?) {
        if (uri == null) return

        val mapper = ObjectMapper()
        val json = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().use { reader -> reader.readText() }
        }.orEmpty()

        val backupFile = mapper.readValue(json, BackupFile::class.java)

        query {
            Database.watchHistoryDao().insertAll(
                *backupFile.watchHistory.orEmpty().toTypedArray()
            )
            Database.searchHistoryDao().insertAll(
                *backupFile.searchHistory.orEmpty().toTypedArray()
            )
            Database.watchPositionDao().insertAll(
                *backupFile.watchPositions.orEmpty().toTypedArray()
            )
            Database.localSubscriptionDao().insertAll(
                *backupFile.localSubscriptions.orEmpty().toTypedArray()
            )
            Database.customInstanceDao().insertAll(
                *backupFile.customInstances.orEmpty().toTypedArray()
            )
            Database.playlistBookmarkDao().insertAll(
                *backupFile.playlistBookmarks.orEmpty().toTypedArray()
            )

            backupFile.localPlaylists?.forEach {
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
                when (it.value) {
                    is Boolean -> putBoolean(it.key, it.value)
                    is Float -> putFloat(it.key, it.value)
                    is Long -> putLong(it.key, it.value)
                    is Int -> {
                        when (it.key) {
                            PreferenceKeys.START_FRAGMENT -> putInt(it.key, it.value)
                            else -> putLong(it.key, it.value.toLong())
                        }
                    }
                    is String -> putString(it.key, it.value)
                }
            }
        }
    }
}
