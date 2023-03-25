package com.github.libretube.extensions

import android.icu.text.CompactDecimalFormat
import android.os.Build
import java.util.*
import kotlin.math.pow

fun Long?.formatShort(): String {
    val value = this ?: 0
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        CompactDecimalFormat
            .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
            .format(value)
    } else {
        val units = arrayOf("", "K", "M", "B", "T")
        for (i in units.size downTo 1) {
            val step = 1000.0.pow(i.toDouble())
            if (value > step) return String.format("%3.0f%s", value / step, units[i]).trim()
        }
        value.toString()
    }
}
