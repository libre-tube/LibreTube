package com.github.libretube.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.google.common.math.IntMath.pow
import kotlinx.datetime.toJavaLocalDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Date
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

    val defaultPlaylistName get() = Date().toString()

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
    fun String.toTimeInSeconds(): Long? = parseTimeString(this)?.toLong()

    fun String.parseDurationString(): Float? = parseTimeString(this)

    private fun parseTimeString(timeString: String): Float? {
        if (timeString.all { it.isDigit() }) return timeString.toLongOrNull()?.toFloat()

        if (timeString.all { it.isDigit() || ",.:".contains(it) }) {
            var secondsTotal = 0
            var secondsScoped = 0

            var milliseconds = 0
            var inMillis = false

            for (char in timeString) {
                if (inMillis) {
                    if (!char.isDigit()) break

                    milliseconds *= 10
                    milliseconds += char.digitToInt()
                } else if (char.isDigit()) {
                    secondsScoped *= 10
                    secondsScoped += char.digitToInt()
                } else if (char == ':') {
                    secondsTotal += secondsScoped * 60
                    secondsScoped = 0
                } else if (",.".contains(char)) {
                    secondsTotal += secondsScoped
                    secondsScoped = 0
                    inMillis = true
                }
            }

            val millisDecimal = milliseconds.toFloat() / pow(10, milliseconds.toString().length)
            return secondsTotal.toFloat() + millisDecimal
        }

        return Duration.parseOrNull(timeString)?.inWholeMilliseconds?.toFloat()?.div(1000)
    }

    /**
     * Get video id if the link is a valid youtube video link
     */
    fun getVideoIdFromUri(uri: Uri) = when (uri.host) {
        "www.youtube.com", "m.youtube.com", "piped.video" -> uri.getQueryParameter("v")
        "youtu.be" -> uri.lastPathSegment
        else -> null
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

    fun getUserAgent(context: Context): String {
        return "${context.packageName}/${BuildConfig.VERSION_NAME}"
    }
}
