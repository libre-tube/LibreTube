package com.github.libretube.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.query
import com.github.libretube.obj.BackupFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Backup and restore the preferences
 */
class BackupHelper(private val context: Context) {
    /**
     * Backup the default shared preferences to a file
     */
    fun backupSharedPreferences(uri: Uri?) {
        if (uri == null) return
        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                ObjectOutputStream(FileOutputStream(it.fileDescriptor)).use { output ->
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    // write all preference objects to the output file
                    output.writeObject(pref.all)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * restore the default shared preferences from a file
     */
    @Suppress("UNCHECKED_CAST")
    fun restoreSharedPreferences(uri: Uri?) {
        if (uri == null) return
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                ObjectInputStream(FileInputStream(it.fileDescriptor)).use { input ->
                    // map all the preference keys and their values
                    val entries = input.readObject() as Map<String, *>
                    PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
                        // clear the previous settings
                        clear()

                        // decide for each preference which type it is and save it to the
                        // preferences
                        for ((key, value) in entries) {
                            when (value) {
                                is Boolean -> putBoolean(key, value)
                                is Float -> putFloat(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is String -> putString(key, value)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Backup the database
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
     * Restore a database backup
     */
    fun restoreAdvancedBackup(uri: Uri?) {
        if (uri == null) return

        val mapper = ObjectMapper()
        val json = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().use { reader -> reader.readText() }
        }.orEmpty()

        val backupFile = mapper.readValue(json, BackupFile::class.java)

        query {
            DatabaseHolder.db.watchHistoryDao().insertAll(
                *backupFile.watchHistory?.toTypedArray().orEmpty()
            )
            DatabaseHolder.db.searchHistoryDao().insertAll(
                *backupFile.searchHistory?.toTypedArray().orEmpty()
            )
            DatabaseHolder.db.watchPositionDao().insertAll(
                *backupFile.watchPositions?.toTypedArray().orEmpty()
            )
            DatabaseHolder.db.localSubscriptionDao().insertAll(
                *backupFile.localSubscriptions?.toTypedArray().orEmpty()
            )
            DatabaseHolder.db.customInstanceDao().insertAll(
                *backupFile.customInstances?.toTypedArray().orEmpty()
            )
        }
    }
}
