package com.github.libretube.helpers

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.github.libretube.ui.extensions.toggleSystemBars

object WindowHelper {
    private const val NAVIGATION_MODE = "navigation_mode"

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

    /**
     * Returns [WindowInsetsCompat.Type.systemBars] if the user uses gesture navigation
     * Otherwise returns [WindowInsetsCompat.Type.statusBars]
     */
    fun getGestureControlledBars(context: Context): Int {
        if (Settings.Secure.getInt(context.contentResolver, NAVIGATION_MODE, 0) == 2) {
            return WindowInsetsCompat.Type.systemBars()
        }

        return WindowInsetsCompat.Type.statusBars()
    }

    fun hasCutout(view: View) = ViewCompat.getRootWindowInsets(view)?.displayCutout != null
}
