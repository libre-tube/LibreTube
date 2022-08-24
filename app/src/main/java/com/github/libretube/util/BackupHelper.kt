package com.github.libretube.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Backup and restore the preferences
 */
class BackupHelper(
    private val context: Context
) {

    /**
     * Backup the default shared preferences to a file
     */
    fun backupSharedPreferences(uri: Uri?) {
        if (uri == null) return
        var output: ObjectOutputStream? = null
        try {
            val fileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor
            output = ObjectOutputStream(FileOutputStream(fileDescriptor))
            val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            // write all preference objects to the output file
            output.writeObject(pref.all)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                // close the outputStream
                if (output != null) {
                    output.flush()
                    output.close()
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * restore the default shared preferences from a file
     */
    @Suppress("UNCHECKED_CAST")
    fun restoreSharedPreferences(uri: Uri?) {
        if (uri == null) return
        var input: ObjectInputStream? = null
        try {
            val fileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            input = ObjectInputStream(FileInputStream(fileDescriptor))
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()

            // clear the previous settings
            editor.clear()

            // map all the preference keys and their values
            val entries = input.readObject() as Map<String, *>

            // decide for each preference which type it is and save it to the preferences
            for ((key, value) in entries) {
                if (value is Boolean) {
                    editor.putBoolean(key, value)
                } else if (value is Float) {
                    editor.putFloat(key, value)
                } else if (value is Int) {
                    editor.putInt(key, value)
                } else if (value is Long) {
                    editor.putLong(key, value)
                } else if (value is String) editor.putString(key, value)
            }
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (input != null) {
                    input.close()
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }
}
