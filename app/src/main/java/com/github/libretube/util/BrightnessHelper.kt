package com.github.libretube.util

import android.app.Activity
import android.view.WindowManager
import com.github.libretube.constants.PreferenceKeys
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
            val lp = window.attributes
            lp.screenBrightness = value
            window.attributes = lp
        }

    /**
     * Wrapper for the brightness persisted in the shared preferences.
     */
    private var savedBrightness: Float
        get() = PreferenceHelper.getFloat(PreferenceKeys.PLAYER_SCREEN_BRIGHTNESS, brightness)
        set(value) = PreferenceHelper.putFloat(PreferenceKeys.PLAYER_SCREEN_BRIGHTNESS, value)

    /**
     * Restore screen brightness to device system brightness.
     * [saveOldValue] determines whether the old value should be persisted or not.
     */
    fun resetToSystemBrightness(saveOldValue: Boolean = false) {
        savedBrightness = if (saveOldValue) {
            brightness
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
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
    fun setBrightnessWithScale(value: Float, maxValue: Float, minValue: Float = 0.0f, shouldSave: Boolean = false) {
        brightness = value.normalize(minValue, maxValue, minBrightness, maxBrightness)
        if (shouldSave) savedBrightness = brightness
    }

    /**
     * Get scaled brightness with given range. if [saved] is
     * ture value will be retrived from shared preferences.
     */
    fun getBrightnessWithScale(maxValue: Float, minValue: Float = 0.0f, saved: Boolean = false): Float {
        return if (saved) {
            savedBrightness.normalize(minBrightness, maxBrightness, minValue, maxValue)
        } else {
            brightness.normalize(minBrightness, maxBrightness, minValue, maxValue)
        }
    }
}
