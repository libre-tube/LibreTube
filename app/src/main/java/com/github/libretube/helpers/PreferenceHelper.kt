package com.github.libretube.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.github.libretube.constants.PreferenceKeys
import java.time.Instant

object PreferenceHelper {
    /**
     * for normal preferences
     */
    lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /**
     * For sensitive data (like token)
     */
    private lateinit var authSettings: SharedPreferences
    private lateinit var authEditor: SharedPreferences.Editor

    /**
     * set the context that is being used to access the shared preferences
     */
    fun initialize(context: Context) {
        settings = getDefaultSharedPreferences(context)
        editor = settings.edit()

        authSettings = getAuthenticationPreferences(context)
        authEditor = authSettings.edit()
    }

    fun putString(key: String, value: String) {
        editor.putString(key, value).commit()
    }

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).commit()
    }

    fun putInt(key: String, value: Int) {
        editor.putInt(key, value).commit()
    }

    fun putFloat(key: String, value: Float) {
        editor.putFloat(key, value).commit()
    }

    fun putLong(key: String, value: Long) {
        editor.putLong(key, value).commit()
    }

    fun getString(key: String?, defValue: String): String {
        return settings.getString(key, defValue) ?: defValue
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return settings.getBoolean(key, defValue)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return runCatching {
            settings.getInt(key, defValue)
        }.getOrElse { settings.getLong(key, defValue.toLong()).toInt() }
    }

    fun getLong(key: String?, defValue: Long): Long {
        return settings.getLong(key, defValue)
    }

    fun getFloat(key: String?, defValue: Float): Float {
        return settings.getFloat(key, defValue)
    }

    fun getStringSet(key: String?, defValue: Set<String>): Set<String> {
        return settings.getStringSet(key, defValue).orEmpty()
    }

    fun clearPreferences() {
        editor.clear().apply()
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

    fun setLastSeenVideoId(videoId: String) {
        editor.putString(PreferenceKeys.LAST_STREAM_VIDEO_ID, videoId).commit()
    }

    fun getLastSeenVideoId(): String {
        return getString(PreferenceKeys.LAST_STREAM_VIDEO_ID, "")
    }

    fun updateLastFeedWatchedTime() {
        putLong(PreferenceKeys.LAST_WATCHED_FEED_TIME, Instant.now().epochSecond)
    }

    fun getLastCheckedFeedTime(): Long {
        return getLong(PreferenceKeys.LAST_WATCHED_FEED_TIME, 0)
    }

    fun saveErrorLog(log: String) {
        editor.putString(PreferenceKeys.ERROR_LOG, log).commit()
    }

    fun getErrorLog(): String {
        return getString(PreferenceKeys.ERROR_LOG, "")
    }

    fun getIgnorableNotificationChannels(): List<String> {
        return getString(PreferenceKeys.IGNORED_NOTIFICATION_CHANNELS, "").split(",")
    }

    fun isChannelNotificationIgnorable(channelId: String): Boolean {
        return getIgnorableNotificationChannels().any { it == channelId }
    }

    fun toggleIgnorableNotificationChannel(channelId: String) {
        val ignorableChannels = getIgnorableNotificationChannels().toMutableList()
        if (ignorableChannels.contains(channelId)) {
            ignorableChannels.remove(channelId)
        } else {
            ignorableChannels.add(
                channelId,
            )
        }
        editor.putString(
            PreferenceKeys.IGNORED_NOTIFICATION_CHANNELS,
            ignorableChannels.joinToString(","),
        ).apply()
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getAuthenticationPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PreferenceKeys.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }
}
