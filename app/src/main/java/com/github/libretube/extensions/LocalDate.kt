package com.github.libretube.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDate

fun LocalDate.toMillis() = this.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun String.toLocalDateSafe() = this.substring(0, 10).toLocalDate()