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

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val flags = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()
        if (isFullscreen) {
            controller.hide(flags)
        } else {
            controller.show(flags)
        }

        controller.systemBarsBehavior = if (isFullscreen) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        }

        val layoutFlag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (isFullscreen) {
            window.setFlags(layoutFlag, layoutFlag)
        } else {
            window.clearFlags(layoutFlag)
        }
    }
}
