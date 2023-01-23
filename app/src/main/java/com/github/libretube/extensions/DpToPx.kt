package com.github.libretube.extensions

import android.content.res.Resources

/**
 * Convert dp to pixels
 */
fun Int.dpToPx(): Float {
    return this * Resources.getSystem().displayMetrics.density + 0.5f
}
