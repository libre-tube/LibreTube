package com.github.libretube.util

import android.app.Activity
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
import com.github.libretube.preferences.PreferenceKeys
import com.google.android.material.color.DynamicColors

object ThemeHelper {

    /**
     * Set the theme, including accent color and night mode
     */
    fun updateTheme(activity: AppCompatActivity) {
        val themeMode = PreferenceHelper.getString(PreferenceKeys.THEME_MODE, "A")

        updateAccentColor(activity)
        applyDynamicColorsIfEnabled(activity)
        applyPureThemeIfEnabled(activity)
        updateThemeMode(themeMode)
    }

    /**
     * Update the accent color of the app
     */
    private fun updateAccentColor(
        activity: AppCompatActivity
    ) {
        val theme = when (
            PreferenceHelper.getString(
                PreferenceKeys.ACCENT_COLOR,
                "purple"
            )
        ) {
            // set the accent color, use the pure black/white theme if enabled
            "my" -> R.style.BaseTheme
            "red" -> R.style.Theme_Red
            "blue" -> R.style.Theme_Blue
            "yellow" -> R.style.Theme_Yellow
            "green" -> R.style.Theme_Green
            "purple" -> R.style.Theme_Purple
            else -> R.style.Theme_Purple
        }
        activity.setTheme(theme)
    }

    /**
     * apply dynamic colors to the activity
     */
    private fun applyDynamicColorsIfEnabled(activity: AppCompatActivity) {
        if (
            PreferenceHelper.getString(
                PreferenceKeys.ACCENT_COLOR,
                "purple"
            ) == "my"
        ) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    /**
     * apply the pure black/white theme
     */
    private fun applyPureThemeIfEnabled(activity: Activity) {
        val pureThemeEnabled = PreferenceHelper.getBoolean(
            PreferenceKeys.PURE_THEME,
            false
        )
        if (pureThemeEnabled) activity.theme.applyStyle(R.style.Pure, true)
    }

    /**
     * set the theme mode (light, dark, auto)
     */
    private fun updateThemeMode(themeMode: String) {
        val mode = when (themeMode) {
            "A" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            "L" -> AppCompatDelegate.MODE_NIGHT_NO
            "D" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * change the app icon
     */
    fun changeIcon(context: Context, newLogoActivityAlias: String) {
        val activityAliases = context.resources.getStringArray(R.array.iconsValue)
        // Disable Old Icon(s)
        for (activityAlias in activityAliases) {
            val activityClass = "com.github.libretube." +
                if (activityAlias == activityAliases[0]) {
                    "activities.MainActivity" // default icon/activity
                } else {
                    activityAlias
                }

            // remove old icons
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, activityClass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // set the class name for the activity alias
        val newLogoActivityClass = "com.github.libretube." +
            if (newLogoActivityAlias == activityAliases[0]) {
                "activities.MainActivity" // default icon/activity
            } else {
                newLogoActivityAlias
            }
        // Enable New Icon
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, newLogoActivityClass),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Needed due to different MainActivity Aliases because of the app icons
     */
    fun restartMainActivity(context: Context) {
        // kill player notification
        val nManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        // start a new Intent of the app
        val pm: PackageManager = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        // kill the old application
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    /**
     * Get a color by a resource code
     */
    fun getThemeColor(context: Context, colorCode: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(colorCode, value, true)
        return value.data
    }

    /**
     * Get the styled app name
     */
    fun getStyledAppName(context: Context): Spanned {
        val colorPrimary = getThemeColor(context, R.attr.colorPrimaryDark)
        val hexColor = String.format("#%06X", (0xFFFFFF and colorPrimary))
        return HtmlCompat.fromHtml(
            "Libre<span  style='color:$hexColor';>Tube</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }
}
