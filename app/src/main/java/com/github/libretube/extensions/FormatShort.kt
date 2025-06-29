package com.github.libretube.extensions

import android.icu.text.CompactDecimalFormat
import java.util.Locale

fun Long?.formatShort(): String = CompactDecimalFormat
    .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
    .format(this ?: 0)
