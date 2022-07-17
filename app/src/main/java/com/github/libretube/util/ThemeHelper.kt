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
        val themeMode = PreferenceHelper.getString("theme_toggle", "A")!!
        val pureThemeEnabled = PreferenceHelper.getBoolean("pure_theme", false)

        updateAccentColor(activity, pureThemeEnabled)
        updateThemeMode(themeMode)
    }

    private fun updateAccentColor(
        activity: AppCompatActivity,
        pureThemeEnabled: Boolean
    ) {
        val theme = when (
            PreferenceHelper.getString(
                "accent_color",
                "purple"
            )
        ) {
            "my" -> {
                applyDynamicColors(activity)
                if (pureThemeEnabled) R.style.MaterialYou_Pure
                else R.style.MaterialYou
            }
            // set the theme, use the pure theme if enabled
            "red" -> if (pureThemeEnabled) R.style.Theme_Red_Pure else R.style.Theme_Red
            "blue" -> if (pureThemeEnabled) R.style.Theme_Blue_Pure else R.style.Theme_Blue
            "yellow" -> if (pureThemeEnabled) R.style.Theme_Yellow_Pure else R.style.Theme_Yellow
            "green" -> if (pureThemeEnabled) R.style.Theme_Green_Pure else R.style.Theme_Green
            "purple" -> if (pureThemeEnabled) R.style.Theme_Purple_Pure else R.style.Theme_Purple
            else -> if (pureThemeEnabled) R.style.Theme_Purple_Pure else R.style.Theme_Purple
        }
        activity.setTheme(theme)
    }

    private fun applyDynamicColors(activity: AppCompatActivity) {
        /**
         * apply dynamic colors to the activity
         */
        DynamicColors.applyToActivityIfAvailable(activity)
    }

    private fun updateThemeMode(themeMode: String) {
        val mode = when (themeMode) {
            "A" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "L" -> AppCompatDelegate.MODE_NIGHT_NO
            "D" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
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
