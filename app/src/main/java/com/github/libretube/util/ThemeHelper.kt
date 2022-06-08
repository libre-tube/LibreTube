package com.github.libretube.util

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.github.libretube.R

class ThemeHelper {

    fun updateTheme(context: Context) {
        updateAccentColor(context)
        updateThemeMode(context)
    }

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
            "O" -> {
                context.setTheme(R.style.OLED)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    fun changeIcon(context: Context, newLogoActivityAlias: String) {
        val activityAliases = context.resources.getStringArray(R.array.iconsValue)
        // Disable Old Icon(s)
        for (activityAlias in activityAliases) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, "com.github.libretube.$activityAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        // Enable New Icon
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, "com.github.libretube.$newLogoActivityAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    // Needed due to different MainActivity Aliases because of the app icons
    fun restartMainActivity(context: Context) {
        // kill player notification
        val nManager = context
            .getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        // restart to MainActivity
        val pm: PackageManager = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
