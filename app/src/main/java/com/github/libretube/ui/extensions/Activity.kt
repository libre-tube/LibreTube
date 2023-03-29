package com.github.libretube.ui.extensions

import android.app.Activity
import android.content.res.Configuration
import androidx.core.view.WindowCompat

val Activity.isLightTheme: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> false
            Configuration.UI_MODE_NIGHT_NO -> true
            else -> true
        }
    }

fun Activity.setLightStatusBarIcons() {
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
}

fun Activity.setDarkStatusBarIcons() {
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
}
