package com.github.libretube.util

import android.app.Activity
import android.view.WindowManager
import com.github.libretube.extensions.normalize

class BrightnessHelper(activity: Activity) {

    private val window = activity.window
    private val minBrightness = 0.0f
    private val maxBrightness = 1.0f

    /**
     * Wrapper for the current screen brightness
     */
    var brightness: Float
        get() = window.attributes.screenBrightness
        private set(value) {
            val lp = window.attributes
            lp.screenBrightness = value
            window.attributes = lp
        }

    /**
     * Restore screen brightness to device system brightness.
     */
    fun resetToSystemBrightness() {
        brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    fun setBrightnessWithScale(value: Float, maxValue: Float, minValue: Float = 0.0f) {
        brightness = value.normalize(minValue, maxValue, minBrightness, maxBrightness)
    }

    fun getBrightnessWithScale(maxValue: Float, minValue: Float = 0.0f): Float {
        return brightness.normalize(minBrightness, maxBrightness, minValue, maxValue)
    }
}
