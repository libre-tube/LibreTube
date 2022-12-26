package com.github.libretube.util

import java.net.URL

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

    /**
     * Check whether an Url is valid
     * @param url The url to test
     * @return Whether the URL is valid
     */
    fun validateUrl(url: String): Boolean {
        runCatching {
            URL(url).toURI()
            return true
        }
        return false
    }
}
