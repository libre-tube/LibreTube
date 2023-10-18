package com.github.libretube.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

fun LocalDate.toMillis() = this.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
