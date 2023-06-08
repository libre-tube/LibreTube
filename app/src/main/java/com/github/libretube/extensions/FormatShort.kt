package com.github.libretube.extensions

import android.icu.text.CompactDecimalFormat
import android.os.Build
import com.github.libretube.helpers.LocaleHelper
import java.util.*
import kotlin.math.pow

fun Long?.formatShort(): String {
    val value = this ?: 0
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        CompactDecimalFormat
            .getInstance(LocaleHelper.getAppLocale(), CompactDecimalFormat.CompactStyle.SHORT)
            .format(value)
    } else {
        val units = arrayOf("", "K", "M", "B", "T")
        for (i in units.size downTo 1) {
            val step = 1000.0.pow(i.toDouble())
            if (value > step) return "%3.0f%s".format(value / step, units[i]).trim()
        }
        value.toString()
    }
}
