package com.github.libretube.extensions

import android.content.res.Resources

/**
 * Convert DP to pixels
 */
fun Int.toPixel(): Float {
    return this * Resources.getSystem().displayMetrics.density + 0.5f
}
