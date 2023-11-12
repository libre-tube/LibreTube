package com.github.libretube.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateUtils
import com.github.libretube.R
import com.github.libretube.ui.dialogs.ShareDialog
import kotlinx.datetime.toJavaLocalDate
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlinx.datetime.LocalDate as KotlinLocalDate

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
    fun localizeDate(date: KotlinLocalDate): String {
        return date.toJavaLocalDate().format(MEDIUM_DATE_FORMATTER)
    }

    /**
     * Get time in seconds from a YouTube video link.
     * @return Time in seconds
     */
    fun String.toTimeInSeconds(): Long? {
        return toLongOrNull() ?: Duration.parseOrNull(this)?.inWholeSeconds
    }

    /**
     * Get video id if the link is a valid youtube video link
     */
    fun getVideoIdFromUrl(link: String): String? {
        val mainPipedFrontendUrl = ShareDialog.PIPED_FRONTEND_URL.toHttpUrl().host
        val unShortenedHosts = listOf("www.youtube.com", "m.youtube.com", mainPipedFrontendUrl)

        return link.toHttpUrlOrNull()?.let {
            when (it.host) {
                in unShortenedHosts -> it.queryParameter("v")
                "youtu.be" -> it.pathSegments.lastOrNull()
                else -> null
            }
        }
    }

    fun formatRelativeDate(context: Context, unixTime: Long): CharSequence {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(unixTime), ZoneId.systemDefault())
        val now = LocalDateTime.now()
        val months = date.until(now, ChronoUnit.MONTHS)

        return if (months > 0) {
            val years = months / 12

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val (timeFormat, time) = if (years > 0) {
                    RelativeDateTimeFormatter.RelativeUnit.YEARS to years
                } else {
                    RelativeDateTimeFormatter.RelativeUnit.MONTHS to months
                }
                RelativeDateTimeFormatter.getInstance()
                    .format(time.toDouble(), RelativeDateTimeFormatter.Direction.LAST, timeFormat)
            } else {
                val (timeAgoRes, time) = if (years > 0) {
                    R.plurals.years_ago to years
                } else {
                    R.plurals.months_ago to months
                }
                context.resources.getQuantityString(timeAgoRes, time.toInt(), time)
            }
        } else {
            val weeks = date.until(now, ChronoUnit.WEEKS)
            val minResolution = if (weeks > 0) DateUtils.WEEK_IN_MILLIS else 0L
            DateUtils.getRelativeTimeSpanString(unixTime, System.currentTimeMillis(), minResolution)
        }
    }

    fun formatBitrate(bitrate: Int?): String {
        bitrate ?: return ""
        return "${bitrate / 1024}kbps"
    }

    fun limitTextToLength(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength) + "…"
    }
}
