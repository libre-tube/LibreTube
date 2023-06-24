package com.github.libretube.helpers

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
    private var brightness: Float
        get() = window.attributes.screenBrightness
        set(value) {
            window.attributes = window.attributes.apply {
                screenBrightness = value
            }
        }

    /**
     * Wrapper for the brightness saved per session, set to the current screen brightness of the
     * beginning of each session / player creation.
     */
    private var savedBrightness = window.attributes.screenBrightness

    /**
     * Restore screen brightness to device system brightness.
     * if [forced] is false then value will be stored only if it's not
     * [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE] value.
     */
    fun resetToSystemBrightness(forced: Boolean = true) {
        if (forced || brightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            savedBrightness = brightness
        }
        brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    /**
     * Set current screen brightness to saved brightness value.
     */
    fun restoreSavedBrightness() {
        brightness = savedBrightness
    }

    /**
     * Set current brightness value with scaling to given range.
     * [shouldSave] determines whether the value should be persisted.
     */
    fun setBrightnessWithScale(
        value: Float,
        maxValue: Float,
        minValue: Float = 0.0f,
        shouldSave: Boolean = false
    ) {
        brightness = value.normalize(minValue, maxValue, minBrightness, maxBrightness)
        if (shouldSave) savedBrightness = brightness
    }

    /**
     * Get scaled brightness with given range. if [saved] is
     * true value will be restored from the session (per played queue)
     */
    fun getBrightnessWithScale(
        maxValue: Float,
        minValue: Float = 0.0f,
        saved: Boolean = false
    ): Float {
        return if (saved) {
            savedBrightness.normalize(minBrightness, maxBrightness, minValue, maxValue)
        } else {
            brightness.normalize(minBrightness, maxBrightness, minValue, maxValue)
        }
    }
}
