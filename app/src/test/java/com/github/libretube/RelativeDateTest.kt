package com.github.libretube

import com.github.libretube.util.TextUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDateTest {
    @Test
    fun reportOneWeek() {
        val zone = ZoneId.of("America/New_York")
        val now = LocalDateTime.of(2025, 3, 12, 12, 0)
        val sevenago = ZonedDateTime.of(2025, 3, 5, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(ChronoUnit.WEEKS to 1L, TextUtils.getRelativeTimeUnit(sevenago, now, zone))
    }
}
