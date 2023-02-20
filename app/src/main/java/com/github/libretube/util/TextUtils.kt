package com.github.libretube.util

import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateUtils
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.time.Duration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object TextUtils {
    /**
     * Separator used for descriptions
     */
    const val SEPARATOR = " • "

    /**
     * Reserved characters by unix which can not be used for file name.
     */
    const val RESERVED_CHARS = "?:\"*|/\\<>\u0000"

    private const val weekInMillis: Long = 604800016
    private const val monthInMillis: Long = 2629800000
    private const val yearInMillis: Long = 31557600000

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

    /**
     * Get time in seconds from a youtube video link
     * @param t The time string to parse
     * @return Time in seconds
     */
    fun parseTimestamp(t: String): Long? {
        return t.toLongOrNull() ?: Duration.parseOrNull(t)?.inWholeSeconds
    }

    /**
     * Get video id if the link is a valid youtube video link
     */
    fun getVideoIdFromUri(link: String): String? {
        return link.toHttpUrlOrNull()?.let {
            when (it.host) {
                "www.youtube.com" -> it.queryParameter("v")
                "youtu.be" -> it.pathSegments.lastOrNull()
                else -> null
            }
        }
    }

    fun formatRelativeDate(unixTime: Long): CharSequence {
        val timeDiff = Instant.now().toEpochMilli() - unixTime
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && timeDiff > weekInMillis) {
            val fmt = RelativeDateTimeFormatter.getInstance()
            val (timeFormat, time) = when {
                timeDiff >= yearInMillis -> RelativeDateTimeFormatter.RelativeUnit.YEARS to timeDiff / yearInMillis
                timeDiff >= monthInMillis -> RelativeDateTimeFormatter.RelativeUnit.MONTHS to timeDiff / monthInMillis
                else -> RelativeDateTimeFormatter.RelativeUnit.WEEKS to timeDiff / weekInMillis
            }
            fmt.format(time.toDouble(), RelativeDateTimeFormatter.Direction.LAST, timeFormat)
        } else {
            DateUtils.getRelativeTimeSpanString(unixTime)
        }
    }
}
