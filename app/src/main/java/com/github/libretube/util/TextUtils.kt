package com.github.libretube.util

object TextUtils {
    /**
     * Separator used for descriptions
     */
    const val SEPARATOR = " • "

    /**
     * Regex to check for e-mails
     */
    const val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"

    fun toTwoDecimalsString(num: Int): String {
        return if (num >= 10) num.toString() else "0$num"
    }
}
