package com.github.libretube.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper

object PreferenceHelper {
    /**
     * for normal preferences
     */
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /**
     * For sensitive data (like token)
     */
    private lateinit var authSettings: SharedPreferences
    private lateinit var authEditor: SharedPreferences.Editor

    private val mapper = ObjectMapper()

    /**
     * set the context that is being used to access the shared preferences
     */
    fun setContext(context: Context) {
        settings = getDefaultSharedPreferences(context)
        editor = settings.edit()

        authSettings = getAuthenticationPreferences(context)
        authEditor = authSettings.edit()
    }

    fun putString(key: String?, value: String) {
        editor.putString(key, value)
    }

    fun getString(key: String?, defValue: String?): String {
        return settings.getString(key, defValue)!!
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return settings.getBoolean(key, defValue)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return settings.getInt(key, defValue)
    }

    fun clearPreferences() {
        editor.clear().apply()
    }

    fun removePreference(value: String?) {
        editor.remove(value).apply()
    }

    fun getToken(): String {
        return authSettings.getString(PreferenceKeys.TOKEN, "")!!
    }

    fun setToken(newValue: String) {
        authEditor.putString(PreferenceKeys.TOKEN, newValue).apply()
    }

    fun getUsername(): String {
        return authSettings.getString(PreferenceKeys.USERNAME, "")!!
    }

    fun setUsername(newValue: String) {
        authEditor.putString(PreferenceKeys.USERNAME, newValue).apply()
    }

    fun setLatestVideoId(videoId: String) {
        editor.putString(PreferenceKeys.LAST_STREAM_VIDEO_ID, videoId).commit()
    }

    fun getLatestVideoId(): String {
        return getString(PreferenceKeys.LAST_STREAM_VIDEO_ID, "")
    }

    fun saveErrorLog(log: String) {
        editor.putString(PreferenceKeys.ERROR_LOG, log).commit()
    }

    fun getErrorLog(): String {
        return getString(PreferenceKeys.ERROR_LOG, "")
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getAuthenticationPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PreferenceKeys.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }
}
