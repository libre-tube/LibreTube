package com.github.libretube.util

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.Spanned
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import com.github.libretube.R
import com.github.libretube.preferences.PreferenceHelper
import com.google.android.material.color.DynamicColors

object ThemeHelper {

    fun updateTheme(activity: AppCompatActivity) {
        updateAccentColor(activity)
        updateThemeMode(activity)
    }

    private fun updateAccentColor(activity: AppCompatActivity) {
        val theme = when (
            PreferenceHelper.getString(
                activity,
                "accent_color",
                "purple"
            )
        ) {
            "my" -> {
                applyDynamicColors(activity)
                R.style.MaterialYou
            }
            "red" -> R.style.Theme_Red
            "blue" -> R.style.Theme_Blue
            "yellow" -> R.style.Theme_Yellow
            "green" -> R.style.Theme_Green
            "purple" -> R.style.Theme_Purple
            else -> R.style.Theme_Purple
        }
        activity.setTheme(theme)
    }

    private fun applyDynamicColors(activity: AppCompatActivity) {
        /**
         * apply dynamic colors to the activity
         */
        DynamicColors.applyToActivityIfAvailable(activity)
    }

    private fun updateThemeMode(context: Context) {
        when (PreferenceHelper.getString(context, "theme_togglee", "A")) {
            "A" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "L" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "D" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "O" -> {
                context.setTheme(R.style.Black)
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

    fun getThemeColor(context: Context, colorCode: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(colorCode, value, true)
        return value.data
    }

    fun getStyledAppName(context: Context): Spanned {
        val colorPrimary = getThemeColor(context, R.attr.colorPrimaryDark)
        val hexColor = String.format("#%06X", (0xFFFFFF and colorPrimary))
        return HtmlCompat.fromHtml(
            "Libre<span  style='color:$hexColor';>Tube</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }
}
