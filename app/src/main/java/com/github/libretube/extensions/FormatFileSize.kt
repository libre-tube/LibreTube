package com.github.libretube.extensions

import java.io.File
import kotlin.math.log2
import kotlin.math.pow

fun File.formatSize(): String {
    return length().formatAsFileSize()
}

fun Int.formatAsFileSize(): String {
    return toLong().formatAsFileSize()
}

fun Long.formatAsFileSize(): String {
    return log2(if (this != 0L) toDouble() else 1.0).toInt().div(10).let {
        val precision = when (it) {
            0 -> 0; 1 -> 1; else -> 2
        }
        val prefix = arrayOf("", "K", "M", "G", "T", "P", "E", "Z", "Y")
        String.format("%.${precision}f ${prefix[it]}B", toDouble() / 2.0.pow(it * 10.0))
    }
}
