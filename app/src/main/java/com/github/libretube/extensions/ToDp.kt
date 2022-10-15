package com.github.libretube.extensions

import android.content.res.Resources

fun Int.toDp(resources: Resources): Float {
    val scale = resources.displayMetrics.density
    return this * scale + 0.5f
}
