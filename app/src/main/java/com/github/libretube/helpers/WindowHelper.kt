package com.github.libretube.helpers

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object WindowHelper {
    fun toggleFullscreen(activity: Activity, isFullscreen: Boolean) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (isFullscreen) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)

        if (isFullscreen) {
            activity.hideSystemBars()
        } else {
            activity.showSystemBars()
        }
    }
}

fun Activity.hideSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
    }
}

fun Activity.showSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        show(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
    }
}
