package com.github.libretube.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.obj.CustomInstance

object PreferenceHelper {
    private val TAG = "PreferenceHelper"

    private lateinit var prefContext: Context

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
        prefContext = context

        settings = getDefaultSharedPreferences(prefContext)
        editor = settings.edit()

        authSettings = getAuthenticationPreferences(context)
        authEditor = authSettings.edit()
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
            val json = settings.getString("search_history", "")!!
            val type = object : TypeReference<List<String>>() {}
            return mapper.readValue(json, type)
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
        val json = mapper.writeValueAsString(historyList)
        editor.putString("search_history", json).apply()
    }

    fun setLatestVideoId(videoId: String) {
        editor.putString(PreferenceKeys.LAST_STREAM_VIDEO_ID, videoId)
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

    fun getLocalSubscriptions(): List<String> {
        val json = settings.getString(PreferenceKeys.LOCAL_SUBSCRIPTIONS, "")
        return try {
            val type = object : TypeReference<List<String>>() {}
            mapper.readValue(json, type)
        } catch (e: Exception) {
            listOf()
        }
    }

    fun setLocalSubscriptions(channels: List<String>) {
        val json = mapper.writeValueAsString(channels)
        editor.putString(PreferenceKeys.LOCAL_SUBSCRIPTIONS, json).commit()
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getAuthenticationPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PreferenceKeys.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }
}
