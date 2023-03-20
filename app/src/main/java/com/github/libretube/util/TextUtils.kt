package com.github.libretube.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateUtils
import com.github.libretube.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
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

    /**
     * Date time formatter which uses the [FormatStyle.MEDIUM] format style.
     */
    private val MEDIUM_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    /**
     * Localize the date from a date string, using the medium format.
     * @param date The date to parse
     * @return localized date string
     */
    fun localizeDate(date: LocalDate): String {
        return date.toJavaLocalDate().format(MEDIUM_DATE_FORMATTER)
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

    fun formatRelativeDate(context: Context, unixTime: Long): CharSequence {
        // TODO: Use LocalDate.ofInstant() when it is available in SDK 34.
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(unixTime), ZoneId.systemDefault())
            .toLocalDate()
        val now = java.time.LocalDate.now()
        val weeks = date.until(now, ChronoUnit.WEEKS)

        return if (weeks > 0) {
            val months = date.until(now, ChronoUnit.MONTHS)
            val years = months / 12

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val (timeFormat, time) = when {
                    years > 0 -> RelativeDateTimeFormatter.RelativeUnit.YEARS to years
                    months > 0 -> RelativeDateTimeFormatter.RelativeUnit.MONTHS to months
                    else -> RelativeDateTimeFormatter.RelativeUnit.WEEKS to weeks
                }
                RelativeDateTimeFormatter.getInstance()
                    .format(time.toDouble(), RelativeDateTimeFormatter.Direction.LAST, timeFormat)
            } else {
                val (timeAgoRes, time) = when {
                    years > 0 -> R.plurals.years_ago to years
                    months > 0 -> R.plurals.months_ago to months
                    else -> R.plurals.weeks_ago to weeks
                }
                context.resources.getQuantityString(timeAgoRes, time.toInt(), time)
            }
        } else {
            DateUtils.getRelativeTimeSpanString(unixTime)
        }
    }

    fun formatBitrate(bitrate: Int?): String {
        bitrate ?: return ""
        return "${bitrate / 1024}kbps"
    }
}
