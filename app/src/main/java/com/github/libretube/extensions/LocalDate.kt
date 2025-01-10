package com.github.libretube.extensions

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

fun LocalDate.toMillis() = this.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun Long.toLocalDateTime() =
    Instant.fromEpochMilliseconds(this).toLocalDateTime()

fun Long.toLocalDate() =
    Instant.fromEpochMilliseconds(this).toLocalDate()

fun Instant.toLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())

fun Instant.toLocalDate() = this.toLocalDateTime().date