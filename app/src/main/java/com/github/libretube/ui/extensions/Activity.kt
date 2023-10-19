package com.github.libretube.ui.extensions

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsControllerCompat

fun Window.toggleSystemBars(@InsetsType types: Int, showBars: Boolean) {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (showBars) {
            show(types)
        } else {
            hide(types)
        }
    }
}
