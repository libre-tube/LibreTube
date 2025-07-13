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
     * Wrapper for the brightness saved per session.
     * Used to restore the previous fullscreen brightness when entering fullscreen.
     */
    private var savedBrightness = window.attributes.screenBrightness

    /**
     * Restore screen brightness to device system brightness.
     */
    fun resetToSystemBrightness() {
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
     */
    fun setBrightnessWithScale(
        value: Float,
        maxValue: Float,
        minValue: Float = 0.0f
    ) {
        brightness = linearToGamma(
            value.normalize(
                minValue,
                maxValue,
                minBrightness,
                maxBrightness
            )
        )

        // remember brightness to restore it when fullscreen is entered again
        savedBrightness = brightness
    }

    /**
     * Logarithmically scale brightness changes, in order to have more control over the lower brightness area
     * @param value brightness value in the interval [0, 1]
     * @return scaled brightness value in the same interval [0, 1]
     *
     * see https://tunjid.medium.com/reverse-engineering-android-pies-logarithmic-brightness-curve-ecd41739d7a2
     * and resolve the formula to x=... the constants are slightly adjusted to actually reach 100% max
     */
    private fun linearToGamma(value: Float): Float {
        // original formula: (Math.exp((value*100+9.411)/19.811) / 255.0).toFloat()
        return (Math.exp((value * LINEAR_MAX + LINEAR_OFFSET)/ SCALING_FACTOR) / GAMMA_MAX).toFloat()
    }

    /**
     * Inverse method for [linearToGamma]
     */
    private fun gammaToLinear(value: Float): Float {
        return ((SCALING_FACTOR * Math.log(value * GAMMA_MAX) - LINEAR_OFFSET) / LINEAR_MAX).toFloat()
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
        val value = if (saved) savedBrightness else brightness

        val scaled = gammaToLinear(value)
            .normalize(minBrightness, maxBrightness, minValue, maxValue)
        return scaled
    }

    companion object {
        // constants only used for linear-gamma conversion
        private const val GAMMA_MAX = 255.0
        private const val LINEAR_MAX = 100.0
        private const val LINEAR_OFFSET = 9.7
        private const val SCALING_FACTOR = 19.9
    }
}
