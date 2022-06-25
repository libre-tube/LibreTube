package com.github.libretube.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object PreferenceHelper {
    fun setString(context: Context, key: String?, value: String?) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.putString(key, value)
        editor.apply()
    }

    fun setInt(context: Context, key: String?, value: Int) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.putInt(key, value)
        editor.apply()
    }

    fun setLong(context: Context, key: String?, value: Long) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.putLong(key, value)
        editor.apply()
    }

    fun setBoolean(context: Context, key: String?, value: Boolean) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getString(context: Context, key: String?, defValue: String?): String? {
        val settings: SharedPreferences = getDefaultSharedPreferences(context)
        return settings.getString(key, defValue)
    }

    fun getInt(context: Context, key: String?, defValue: Int): Int {
        val settings: SharedPreferences = getDefaultSharedPreferences(context)
        return settings.getInt(key, defValue)
    }

    fun getLong(context: Context, key: String?, defValue: Long): Long {
        val settings: SharedPreferences = getDefaultSharedPreferences(context)
        return settings.getLong(key, defValue)
    }

    fun getBoolean(context: Context, key: String?, defValue: Boolean): Boolean {
        val settings: SharedPreferences = getDefaultSharedPreferences(context)
        return settings.getBoolean(key, defValue)
    }

    fun clearPreferences(context: Context) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.clear()
        editor.commit()
    }

    fun removePreference(context: Context, value: String?) {
        val editor = getDefaultSharedPreferencesEditor(context)
        editor.remove(value)
        editor.commit()
    }

    private fun getDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    private fun getDefaultSharedPreferencesEditor(context: Context): SharedPreferences.Editor {
        return getDefaultSharedPreferences(context).edit()
    }
}
