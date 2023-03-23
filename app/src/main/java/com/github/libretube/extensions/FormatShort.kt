package com.github.libretube.extensions

import kotlin.math.pow

fun Long?.formatShort(): String {
    this ?: return (0).toString()
    val units = arrayOf("", "K", "M", "B", "T")

    for (i in units.size downTo 1) {
        val step = 1000.0.pow(i.toDouble())
        if (this > step) return String.format("%3.0f%s", this / step, units[i]).trim()
    }
    return this.toString()
}
