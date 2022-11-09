package com.github.libretube.util

object TextUtils {
    /**
     * Separator used for descriptions
     */
    const val SEPARATOR = " • "

    fun toTwoDecimalsString(num: Int): String {
        return if (num >= 10) num.toString() else "0$num"
    }
}
