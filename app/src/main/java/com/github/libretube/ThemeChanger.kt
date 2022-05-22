package com.github.libretube

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import java.util.Locale

fun updateAccentColor(context: Context) {
    val colorAccent =
        PreferenceManager.getDefaultSharedPreferences(context).getString("accent_color", "red")
    when (colorAccent) {
        "my" -> context.setTheme(R.style.Theme_MY)
        "red" -> context.setTheme(R.style.Theme_Red)
        "blue" -> context.setTheme(R.style.Theme_Blue)
        "yellow" -> context.setTheme(R.style.Theme_Yellow)
        "green" -> context.setTheme(R.style.Theme_Green)
        "purple" -> context.setTheme(R.style.Theme_Purple)
    }
}

fun updateThemeMode(context: Context) {
    val themeMode =
        PreferenceManager.getDefaultSharedPreferences(context).getString("theme_togglee", "A")
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
        var locale = if (languageName != "sys" && "$languageName".length < 3) {
            Locale(languageName)
        } else if ("$languageName".length > 3) {
            Locale(languageName?.substring(0, 2), languageName?.substring(4, 6))
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

fun changeIcon(context: Context) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val newLogoActivityAlias = sharedPreferences.getString("icon_change", "com.github.libretube.MainActivity")
    val activityAliases = context?.resources.getStringArray(R.array.iconsValue)
    // Disable Old Icon(s)
    for (activityAlias in activityAliases) {
        context?.packageManager.setComponentEnabledSetting(
            ComponentName(context?.packageName, activityAlias),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
    // Enable New Icon
    context?.packageManager.setComponentEnabledSetting(
        ComponentName(context?.packageName, newLogoActivityAlias!!),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}
