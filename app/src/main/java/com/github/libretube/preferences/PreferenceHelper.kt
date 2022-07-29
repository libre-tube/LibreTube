package com.github.libretube.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.obj.CustomInstance
import com.github.libretube.obj.Streams
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.obj.WatchPosition
import com.github.libretube.util.toID

object PreferenceHelper {
    private val TAG = "PreferenceHelper"

    private lateinit var prefContext: Context
    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val mapper = ObjectMapper()

    /**
     * set the context that is being used to access the shared preferences
     */
    fun setContext(context: Context) {
        prefContext = context
        settings = getDefaultSharedPreferences(prefContext)
        editor = settings.edit()
    }

    fun getString(key: String?, defValue: String?): String {
        return settings.getString(key, defValue)!!
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
        val customInstancesList = getCustomInstances()
        customInstancesList += customInstance

        val json = mapper.writeValueAsString(customInstancesList)
        editor.putString("customInstances", json).apply()
    }

    fun getCustomInstances(): ArrayList<CustomInstance> {
        val json: String = settings.getString("customInstances", "")!!
        val type = mapper.typeFactory.constructCollectionType(
            List::class.java,
            CustomInstance::class.java
        )
        return try {
            mapper.readValue(json, type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    fun getSearchHistory(): List<String> {
        return try {
            val set: Set<String> = settings.getStringSet("search_history", LinkedHashSet())!!
            set.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveToSearchHistory(query: String) {
        val historyList = getSearchHistory().toMutableList()

        if ((historyList.contains(query))) {
            // remove from history list if already contained
            historyList -= query
        }

        // append new query to history
        historyList.add(0, query)

        if (historyList.size > 10) {
            historyList.removeAt(historyList.size - 1)
        }

        updateSearchHistory(historyList)
    }

    fun removeFromSearchHistory(query: String) {
        val historyList = getSearchHistory().toMutableList()
        historyList -= query
        updateSearchHistory(historyList)
    }

    private fun updateSearchHistory(historyList: List<String>) {
        val set: Set<String> = LinkedHashSet(historyList)
        editor.putStringSet("search_history", set).apply()
    }

    fun addToWatchHistory(videoId: String, streams: Streams) {
        removeFromWatchHistory(videoId)

        val watchHistoryItem = WatchHistoryItem(
            videoId,
            streams.title,
            streams.uploadDate,
            streams.uploader,
            streams.uploaderUrl.toID(),
            streams.uploaderAvatar,
            streams.thumbnailUrl,
            streams.duration
        )

        val watchHistory = getWatchHistory()

        watchHistory += watchHistoryItem

        val json = mapper.writeValueAsString(watchHistory)
        editor.putString("watch_history", json).apply()
    }

    fun removeFromWatchHistory(videoId: String) {
        val watchHistory = getWatchHistory()

        var indexToRemove: Int? = null
        watchHistory.forEachIndexed { index, item ->
            if (item.videoId == videoId) indexToRemove = index
        }
        if (indexToRemove != null) {
            watchHistory.removeAt(indexToRemove!!)
            val json = mapper.writeValueAsString(watchHistory)
            editor.putString("watch_history", json).commit()
        }
    }

    fun getWatchHistory(): ArrayList<WatchHistoryItem> {
        val json: String = settings.getString("watch_history", "")!!
        val type = mapper.typeFactory.constructCollectionType(
            List::class.java,
            WatchHistoryItem::class.java
        )

        return try {
            mapper.readValue(json, type)
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

        val json = mapper.writeValueAsString(watchPositions)
        editor.putString("watch_positions", json).commit()
    }

    fun removeWatchPosition(videoId: String) {
        val watchPositions = getWatchPositions()

        var indexToRemove: Int? = null
        watchPositions.forEachIndexed { index, item ->
            if (item.videoId == videoId) indexToRemove = index
        }

        if (indexToRemove != null) watchPositions.removeAt(indexToRemove!!)

        val json = mapper.writeValueAsString(watchPositions)
        editor.putString("watch_positions", json).commit()
    }

    fun getWatchPositions(): ArrayList<WatchPosition> {
        val json: String = settings.getString("watch_positions", "")!!
        val type = mapper.typeFactory.constructCollectionType(
            List::class.java,
            WatchPosition::class.java
        )

        return try {
            mapper.readValue(json, type)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    fun setLatestVideoId(videoId: String) {
        editor.putString(PreferenceKeys.LAST_STREAM_VIDEO_ID, videoId)
    }

    fun getLatestVideoId(): String {
        return getString(PreferenceKeys.LAST_STREAM_VIDEO_ID, "")
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
