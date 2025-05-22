package com.github.libretube.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.libretube.LibreTubeApp
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.SbSkipOptions

object PreferenceHelper {
    private val TAG = PreferenceHelper::class.simpleName

    /**
     * Preference migration from [fromVersion] to [toVersion].
     */
    private class PreferenceMigration(
        val fromVersion: Int, val toVersion: Int, val onMigration: () -> Unit
    )

    /**
     * for normal preferences
     */
    lateinit var settings: SharedPreferences

    /**
     * For sensitive data (like token)
     */
    private lateinit var authSettings: SharedPreferences

    /**
     * Possible chars to use for the SB User ID
     */
    private const val USER_ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

    /**
     * Migrations required to migrate the application to a newer preference version.
     * The version is automatically determined from the number of migrations available.
     */
    private val MIGRATIONS = arrayOf(
        PreferenceMigration(0, 1) {
            LibreTubeApp.instance.resources
                .getStringArray(R.array.sponsorBlockSegments)
                .forEach { category ->
                    val key = "${category}_category"
                    val stored = getString(key, "visible")
                    if (stored == "visible") {
                        putString(key, SbSkipOptions.MANUAL.name.lowercase())
                    }
                }
        },
    )

    /**
     * set the context that is being used to access the shared preferences
     */
    fun initialize(context: Context) {
        settings = getDefaultSharedPreferences(context)
        authSettings = getAuthenticationPreferences(context)
    }

    /**
     * Migrate preference to a new version.
     */
    fun migrate() {
        var currentPrefVersion = getInt(PreferenceKeys.PREFERENCE_VERSION, 0)

        while (currentPrefVersion < MIGRATIONS.count()) {
            val next = currentPrefVersion + 1

            val migration =
                MIGRATIONS.find { it.fromVersion == currentPrefVersion && it.toVersion == next }
            Log.i(TAG, "Performing migration from $currentPrefVersion to $next")
            migration?.onMigration?.invoke()

            currentPrefVersion++
            // mark as successfully migrated
            putInt(PreferenceKeys.PREFERENCE_VERSION, currentPrefVersion)
        }

    }

    fun putString(key: String, value: String) {
        settings.edit(commit = true) { putString(key, value) }
    }

    fun putBoolean(key: String, value: Boolean) {
        settings.edit(commit = true) { putBoolean(key, value) }
    }

    fun putInt(key: String, value: Int) {
        settings.edit(commit = true) { putInt(key, value) }
    }

    fun putLong(key: String, value: Long) {
        settings.edit(commit = true) { putLong(key, value) }
    }

    fun putStringSet(key: String, value: Set<String>) {
        settings.edit(commit = true) { putStringSet(key, value) }
    }

    fun remove(key: String) {
        settings.edit(commit = true) { remove(key) }
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

    fun getStringSet(key: String?, defValue: Set<String>): Set<String> {
        return settings.getStringSet(key, defValue).orEmpty()
    }

    fun clearPreferences() {
        settings.edit { clear() }
    }

    fun getToken(): String {
        return authSettings.getString(PreferenceKeys.TOKEN, "")!!
    }

    fun setToken(newValue: String) {
        authSettings.edit { putString(PreferenceKeys.TOKEN, newValue) }
    }

    fun getUsername(): String {
        return authSettings.getString(PreferenceKeys.USERNAME, "")!!
    }

    fun setUsername(newValue: String) {
        authSettings.edit { putString(PreferenceKeys.USERNAME, newValue) }
    }

    fun updateLastFeedWatchedTime(time: Long, seenByUser: Boolean) {
        // only update the time if the time is newer
        // this avoids cases, where the user last saw an older video, which had already been seen,
        // causing all following video to be incorrectly marked as unseen again
        if (getLastCheckedFeedTime(false) < time)
            putLong(PreferenceKeys.LAST_REFRESHED_FEED_TIME, time)

        // this value holds the last time the user opened the subscriptions feed
        // whereas [LAST_REFRESHED_FEED_TIME] considers the last time the feed was loaded,
        // which could also be possible in the background (e.g. via notifications)
        if (seenByUser && getLastCheckedFeedTime(true) < time)
            putLong(PreferenceKeys.LAST_USER_SEEN_FEED_TIME, time)
    }

    fun getLastCheckedFeedTime(seenByUser: Boolean): Long {
        val key =
            if (seenByUser) PreferenceKeys.LAST_USER_SEEN_FEED_TIME else PreferenceKeys.LAST_REFRESHED_FEED_TIME
        return getLong(key, 0)
    }

    fun saveErrorLog(log: String) {
        putString(PreferenceKeys.ERROR_LOG, log)
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
            ignorableChannels.add(channelId)
        }
        settings.edit {
            val channelsString = ignorableChannels.joinToString(",")
            putString(PreferenceKeys.IGNORED_NOTIFICATION_CHANNELS, channelsString)
        }
    }

    fun getSponsorBlockUserID(): String {
        var uuid = getString(PreferenceKeys.SB_USER_ID, "")
        if (uuid.isEmpty()) {
            // generate a new user id to use for submitting SponsorBlock segments
            uuid = (0 until 30).map { USER_ID_CHARS.random() }.joinToString("")
            putString(PreferenceKeys.SB_USER_ID, uuid)
        }
        return uuid
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getAuthenticationPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PreferenceKeys.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }
}
