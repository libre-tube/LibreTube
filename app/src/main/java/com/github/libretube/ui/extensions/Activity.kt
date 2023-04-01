package com.github.libretube.ui.extensions

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsControllerCompat

fun Activity.toggleSystemBars(@InsetsType types: Int, showBars: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (showBars) {
            show(types)
        } else {
            hide(types)
        }
    }
}
