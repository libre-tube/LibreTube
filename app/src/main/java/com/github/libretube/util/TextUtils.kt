package com.github.libretube.util

import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

object TextUtils {
    /**
     * Separator used for descriptions
     */
    const val SEPARATOR = " • "

    /**
     * Regex to check for e-mails
     */
    const val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"

    /**
     * Reserved characters by unix which can not be used for file name.
     */
    const val RESERVED_CHARS = "?:\"*|/\\<>\u0000"

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
    fun localizeDate(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return date.toJavaLocalDate().format(formatter)
    }
}
