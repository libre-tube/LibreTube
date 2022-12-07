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

    const val TIMESTAMP_REGEX = "(?:(?:(\\d+):)?([0-5]?\\d):)([0-5]?\\d)"

    fun toTwoDecimalsString(num: Int): String {
        return if (num >= 10) num.toString() else "0$num"
    }

    /**
     * Convert [text] into seconds. If fails to convert returns -1.
     */
    fun toTimeInSeconds(text: String): Long {
        val units = text.split(":")

        try {
            var time = units.last().toLong()
            time += when (units.size) {
                3 -> (units[0].toLong() * 60 * 60) + (units[1].toLong() * 60)
                2 -> (units[0].toLong() * 60)
                else -> 0
            }
            return time
        } catch (_: Exception) { }

        return -1
    }
}
