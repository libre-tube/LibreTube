package com.github.libretube.util

import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

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

    /**
     * Localize the date from a time string
     * @param date The date to parse
     * @param locale The locale to use, otherwise uses system default
     * return Localized date string
     */
    fun localizeDate(date: String?, locale: Locale): String? {
        date ?: return null

        // relative time span
        if (!date.contains("-")) return date

        val dateObj = LocalDate.parse(date)

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return dateObj.format(formatter)
    }
}
