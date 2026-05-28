package com.github.libretube.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.libretube.LibreTubeApp
import com.github.libretube.R
import com.github.libretube.api.TrendingCategory
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.SbSkipOptions
import com.github.libretube.helpers.LocaleHelper.getDetectedCountry
import kotlin.math.roundToInt

object PreferenceHelper {

    private val TAG = PreferenceHelper::class.simpleName

    private const val USER_ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

    lateinit var settings: SharedPreferences
        private set

    private lateinit var authSettings: SharedPreferences

    private data class PreferenceMigration(
        val fromVersion: Int,
        val toVersion: Int,
        val onMigration: () -> Unit
    )

    private val MIGRATIONS = listOf(
        PreferenceMigration(0, 1) {
            LibreTubeApp.instance.resources
                .getStringArray(R.array.sponsorBlockSegments)
                .forEach { category ->
                    val key = "${category}_category"
                    if (getString(key, "visible") == "visible") {
                        putString(key, SbSkipOptions.MANUAL.name.lowercase())
                    }
                }
        },
        PreferenceMigration(1, 2) {
            putString(PreferenceKeys.TRENDING_CATEGORY, TrendingCategory.LIVE.name)
        },
        PreferenceMigration(2, 3) {
            listOf(
                "player_audio_format", "lbry_hls", "confirm_unsubscribing", "legacy_subscriptions",
                "legacy_subscriptions_columns", "filter_history_type", "use_hls", "last_stream_video_id",
                "last_watched_feed_time", "last_feed_refresh_timestamp_millis", "custom_playback_speed",
                "background_playback_speed", "player_resize_mode", "alternative_videos_layout",
                "clearCustomInstances", "auth", "image_proxy_url", "dearrow", "unlimited_search_history",
                "sb_highlights", "audio_only_mode", "sb_contribute_key", "dearrow_contribute_key",
                "sb_user_id", "fallback_piped_proxy", "picture_in_picture", "pause_on_quit", "save_feed",
                "filer_feed", "max_concurrent_downloads", "grid", "sleep_timer_toggle", "sleep_timer_delay",
                "alternative_player_layout", "auto_rotation", "sb_show_markers", "autoplay",
                "sb_skip_manually_key", "player_screen_brightness", "selected_filer_feed", "selected_feed_filer",
                "feed_sort_oder", "player_video_format", "watch_position_toggle", "break_reminder_toggle",
                "break_reminder", "notification_open_queue", "data_saver_mode", "import_from_yt",
                "export_subs", "show_open_with", "player_swipe_control", "progressive_loading_interval",
                "limit_hls", "nav_bar_items", "trending_layout", "default_tab", "backup_settings",
                "restore_settings", "hide_trending_page", "sb_skip_manually", "download_location",
                "download_folder"
            ).forEach { remove(it) }
        },
        PreferenceMigration(3, 4) { listOf("video_codecs", "audio_codecs").forEach { remove(it) } },
        PreferenceMigration(4, 5) { remove("remember_playback_speed") },
        PreferenceMigration(5, 6) {
            settings.getString(PreferenceKeys.PLAYBACK_SPEED, null)?.let { speedStr ->
                val speed = speedStr.replace("F", "").toFloat().roundToNearestQuarter()
                putString(PreferenceKeys.PLAYBACK_SPEED, speed.toString())
            }
        },
        PreferenceMigration(6, 7) { remove("disable_video_image_proxy") }
    )

    fun initialize(context: Context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context)
        authSettings = context.getSharedPreferences(PreferenceKeys.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }

    fun migrate() {
        var version = getInt(PreferenceKeys.PREFERENCE_VERSION, 0)
        while (version < MIGRATIONS.size) {
            MIGRATIONS.find { it.fromVersion == version }?.onMigration?.invoke()
            version++
            putInt(PreferenceKeys.PREFERENCE_VERSION, version)
            Log.i(TAG, "Migrated preferences to version $version")
        }
    }

    // --- PUTTERS ---
    private inline fun SharedPreferences.editNow(action: SharedPreferences.Editor.() -> Unit) =
        edit(commit = true, action = action)

    fun putString(key: String, value: String) = settings.editNow { putString(key, value) }
    fun putBoolean(key: String, value: Boolean) = settings.editNow { putBoolean(key, value) }
    fun putInt(key: String, value: Int) = settings.editNow { putInt(key, value) }
    fun putLong(key: String, value: Long) = settings.editNow { putLong(key, value) }
    fun putStringSet(key: String, value: Set<String>) = settings.editNow { putStringSet(key, value) }
    fun remove(key: String) = settings.editNow { remove(key) }
    fun clearPreferences() = settings.editNow { clear() }

    // --- GETTERS ---
    fun getString(key: String?, defValue: String) = settings.getString(key, defValue) ?: defValue
    fun getBoolean(key: String?, defValue: Boolean) = settings.getBoolean(key, defValue)
    fun getInt(key: String?, defValue: Int) = runCatching { settings.getInt(key, defValue) }
        .getOrElse { settings.getLong(key, defValue.toLong()).toInt() }

    fun getLong(key: String?, defValue: Long) = settings.getLong(key, defValue)
    fun getStringSet(key: String?, defValue: Set<String>) = settings.getStringSet(key, defValue).orEmpty()

    // --- AUTH ---
    fun getToken() = authSettings.getString(PreferenceKeys.TOKEN, "")!!
    fun setToken(value: String) = authSettings.editNow { putString(PreferenceKeys.TOKEN, value) }

    fun getUsername() = authSettings.getString(PreferenceKeys.USERNAME, "")!!
    fun setUsername(value: String) = authSettings.editNow { putString(PreferenceKeys.USERNAME, value) }

    // --- Feed Tracking ---
    fun updateLastFeedWatchedTime(time: Long, seenByUser: Boolean) {
        if (getLastCheckedFeedTime(false) < time) putLong(PreferenceKeys.LAST_REFRESHED_FEED_TIME, time)
        if (seenByUser && getLastCheckedFeedTime(true) < time) putLong(PreferenceKeys.LAST_USER_SEEN_FEED_TIME, time)
    }

    fun getLastCheckedFeedTime(seenByUser: Boolean) =
        getLong(if (seenByUser) PreferenceKeys.LAST_USER_SEEN_FEED_TIME else PreferenceKeys.LAST_REFRESHED_FEED_TIME, 0)

    // --- Error Logging ---
    fun saveErrorLog(log: String) = putString(PreferenceKeys.ERROR_LOG, log)
    fun getErrorLog() = getString(PreferenceKeys.ERROR_LOG, "")

    // --- Notifications ---
    fun getIgnorableNotificationChannels() =
        getString(PreferenceKeys.IGNORED_NOTIFICATION_CHANNELS, "").split(",").filter { it.isNotEmpty() }

    fun isChannelNotificationIgnorable(channelId: String) = getIgnorableNotificationChannels().contains(channelId)

    fun toggleIgnorableNotificationChannel(channelId: String) {
        val channels = getIgnorableNotificationChannels().toMutableSet()
        if (!channels.add(channelId)) channels.remove(channelId)
        putString(PreferenceKeys.IGNORED_NOTIFICATION_CHANNELS, channels.joinToString(","))
    }

    // --- SponsorBlock ---
    fun getSponsorBlockUserID(): String {
        var uuid = getString(PreferenceKeys.SB_USER_ID, "")
        if (uuid.isEmpty()) {
            uuid = (1..30).map { USER_ID_CHARS.random() }.joinToString("")
            putString(PreferenceKeys.SB_USER_ID, uuid)
        }
        return uuid
    }

    // --- Region / Locale ---
    fun getTrendingRegion(context: Context): String =
        if (getString(PreferenceKeys.REGION, "sys") == "sys") getDetectedCountry(context).uppercase()
        else getString(PreferenceKeys.REGION, "sys")

    // --- Helpers ---
    private fun Float.roundToNearestQuarter() = (this * 4).roundToInt() / 4f
}