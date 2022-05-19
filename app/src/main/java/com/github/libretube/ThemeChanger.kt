package com.github.libretube

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.util.*

fun updateAccentColor(context: Context) {
    val colorAccent = PreferenceManager.getDefaultSharedPreferences(context).getString("accent_color", "red")
    when (colorAccent) {
        "red" -> context.setTheme(R.style.Theme_Red)
        "blue" -> context.setTheme(R.style.Theme_Blue)
        "yellow" -> context.setTheme(R.style.Theme_Yellow)
        "green" -> context.setTheme(R.style.Theme_Green)
        "purple" -> context.setTheme(R.style.Theme_Purple)
    }
}

fun updateThemeMode(context: Context) {
    val themeMode = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_togglee", "A")
    when (themeMode) {
        "A" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        "L" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "D" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "O" -> oledMode(context)
    }
}

fun oledMode(context: Context) {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    context.setTheme(R.style.Theme_OLED)
}

fun updateLanguage(context: Context) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val languageName = sharedPreferences.getString("language", "sys")
    if (languageName != "") {
        var locale = if (languageName != "sys" && "$languageName".length < 3 ) {
            Locale(languageName)
        } else if ("$languageName".length > 3) {
            Locale(languageName?.substring(0,2), languageName?.substring(4,6))
        } else {
            Locale.getDefault()
        }
        val res = context.resources
        val dm = res.displayMetrics
        val conf = res.configuration
        conf.setLocale(locale)
        Locale.setDefault(locale)
        res.updateConfiguration(conf, dm)
    }
}