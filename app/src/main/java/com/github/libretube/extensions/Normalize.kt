package com.github.libretube.extensions

fun Int.normalize(oldMin: Int, oldMax: Int, newMin: Int, newMax: Int): Int {
    val oldRange = oldMax - oldMin
    val newRange = newMax - newMin

    return (this - oldMin) * newRange / oldRange + newMin
}

fun Float.normalize(oldMin: Float, oldMax: Float, newMin: Float, newMax: Float): Float {
    val oldRange = oldMax - oldMin
    val newRange = newMax - newMin

    return (this - oldMin) * newRange / oldRange + newMin
}

fun Long.normalize(oldMin: Long, oldMax: Long, newMin: Long, newMax: Long): Long {
    val oldRange = oldMax - oldMin
    val newRange = newMax - newMin

    return (this - oldMin) * newRange / oldRange + newMin
}
