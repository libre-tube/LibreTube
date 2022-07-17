package com.github.libretube.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.github.libretube.obj.CustomInstance
import com.github.libretube.obj.Streams
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.obj.WatchPosition
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.reflect.Type

object PreferenceHelper {
    private val TAG = "PreferenceHelper"

    private lateinit var prefContext: Context
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /**
     * set the context that is being used to access the shared preferences
     */
    fun setContext(context: Context) {
        prefContext = context
        settings = getDefaultSharedPreferences(prefContext)
        editor = settings.edit()
    }

    fun setString(key: String?, value: String?) {
        editor.putString(key, value)
        editor.apply()
    }

    fun setInt(key: String?, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    fun setLong(key: String?, value: Long) {
        editor.putLong(key, value)
        editor.apply()
    }

    fun setBoolean(key: String?, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getString(key: String?, defValue: String?): String? {
        return settings.getString(key, defValue)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return settings.getInt(key, defValue)
    }

    fun getLong(key: String?, defValue: Long): Long {
        return settings.getLong(key, defValue)
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return settings.getBoolean(key, defValue)
    }

    fun clearPreferences() {
        editor.clear().apply()
    }

    fun removePreference(value: String?) {
        editor.remove(value).apply()
    }

    fun getToken(): String {
        val sharedPref = prefContext.getSharedPreferences("token", Context.MODE_PRIVATE)
        return sharedPref?.getString("token", "")!!
    }

    fun setToken(newValue: String) {
        val editor = prefContext.getSharedPreferences("token", Context.MODE_PRIVATE).edit()
        editor.putString("token", newValue).apply()
    }

    fun getUsername(): String {
        val sharedPref = prefContext.getSharedPreferences("username", Context.MODE_PRIVATE)
        return sharedPref.getString("username", "")!!
    }

    fun setUsername(newValue: String) {
        val editor = prefContext.getSharedPreferences("username", Context.MODE_PRIVATE).edit()
        editor.putString("username", newValue).apply()
    }

    fun saveCustomInstance(customInstance: CustomInstance) {
        val gson = Gson()

        val customInstancesList = getCustomInstances()
        customInstancesList += customInstance

        val json = gson.toJson(customInstancesList)
        editor.putString("customInstances", json).apply()
    }

    fun getCustomInstances(): ArrayList<CustomInstance> {
        val gson = Gson()
        val json: String = settings.getString("customInstances", "")!!
        val type: Type = object : TypeToken<List<CustomInstance?>?>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    fun getHistory(): List<String> {
        return try {
            val set: Set<String> = settings.getStringSet("search_history", HashSet())!!
            set.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHistory(historyList: List<String>) {
        val set: Set<String> = HashSet(historyList)
        editor.putStringSet("search_history", set).apply()
    }

    fun addToWatchHistory(videoId: String, streams: Streams) {
        val gson = Gson()

        val watchHistoryItem = WatchHistoryItem(
            videoId,
            streams.title,
            streams.uploadDate,
            streams.uploader,
            streams.uploaderUrl?.replace("/channel/", ""),
            streams.uploaderAvatar,
            streams.thumbnailUrl,
            streams.duration
        )

        val watchHistory = getWatchHistory()

        // delete entries that have the same videoId
        var indexToRemove: Int? = null
        watchHistory.forEachIndexed { index, item ->
            if (item.videoId == videoId) indexToRemove = index
        }
        if (indexToRemove != null) watchHistory.removeAt(indexToRemove!!)

        watchHistory += watchHistoryItem

        val json = gson.toJson(watchHistory)
        editor.putString("watch_history", json).apply()
    }

    fun getWatchHistory(): ArrayList<WatchHistoryItem> {
        val gson = Gson()
        val json: String = settings.getString("watch_history", "")!!
        val type: Type = object : TypeToken<List<WatchHistoryItem?>?>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    fun saveWatchPosition(videoId: String, position: Long) {
        val watchPositions = getWatchPositions()
        val watchPositionItem = WatchPosition(videoId, position)

        var indexToRemove: Int? = null
        watchPositions.forEachIndexed { index, item ->
            if (item.videoId == videoId) indexToRemove = index
        }

        if (indexToRemove != null) watchPositions.removeAt(indexToRemove!!)

        watchPositions += watchPositionItem

        val gson = Gson()
        val json = gson.toJson(watchPositions)
        editor.putString("watch_positions", json).commit()
    }

    fun removeWatchPosition(videoId: String) {
        val watchPositions = getWatchPositions()

        var indexToRemove: Int? = null
        watchPositions.forEachIndexed { index, item ->
            if (item.videoId == videoId) indexToRemove = index
        }

        if (indexToRemove != null) watchPositions.removeAt(indexToRemove!!)

        val gson = Gson()
        val json = gson.toJson(watchPositions)
        editor.putString("watch_positions", json).commit()
    }

    fun getWatchPositions(): ArrayList<WatchPosition> {
        val gson = Gson()

        val json: String = settings.getString("watch_positions", "")!!
        val type: Type = object : TypeToken<List<WatchPosition?>?>() {}.type

        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
