package com.github.libretube.helpers

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.github.libretube.ui.extensions.toggleSystemBars

object WindowHelper {
    fun toggleFullscreen(window: Window, isFullscreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (isFullscreen) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)

        val layoutNoLimitsFlag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (isFullscreen) {
            window.setFlags(layoutNoLimitsFlag, layoutNoLimitsFlag)
        } else {
            window.clearFlags(layoutNoLimitsFlag)
        }

        // Show the system bars when it is not fullscreen and hide them when it is fullscreen
        // System bars means status bar and the navigation bar
        // See: https://developer.android.com/training/system-ui/immersive#kotlin
        window.toggleSystemBars(
            types = WindowInsetsCompat.Type.systemBars(),
            showBars = !isFullscreen
        )
    }
}
