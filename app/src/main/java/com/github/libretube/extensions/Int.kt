package com.github.libretube.extensions

import kotlin.math.ceil

fun Int.ceilHalf() = ceil((toDouble() / 2)).toInt()
