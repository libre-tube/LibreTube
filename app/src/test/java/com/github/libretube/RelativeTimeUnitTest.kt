package com.github.libretube

import com.github.libretube.util.TextUtils.getRelativeTimeUnit
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelativeTimeUnitTest {
    // Fixed reference point so the assertions are deterministic.
    private val now: LocalDateTime = LocalDateTime.of(2026, 6, 19, 12, 0)

    private fun unitFor(date: LocalDateTime) = getRelativeTimeUnit(date, now)

    @Test
    fun lessThanAWeekFallsBackToFinerGrainedFormatter() {
        // null signals the caller to delegate to DateUtils (days/hours/minutes).
        assertNull(unitFor(now))
        assertNull(unitFor(now.minusHours(5)))
        assertNull(unitFor(now.minusDays(1)))
        assertNull(unitFor(now.minusDays(6)))
    }

    @Test
    fun exactlySevenDaysAgoIsOneWeek() {
        // Regression test for #8295: this used to render "0 weeks ago".
        assertEquals(ChronoUnit.WEEKS to 1L, unitFor(now.minusDays(7)))
    }

    @Test
    fun weeksAreReportedForSpansBelowAMonth() {
        assertEquals(ChronoUnit.WEEKS to 1L, unitFor(now.minusDays(13)))
        assertEquals(ChronoUnit.WEEKS to 2L, unitFor(now.minusDays(14)))
        assertEquals(ChronoUnit.WEEKS to 3L, unitFor(now.minusDays(21)))
    }

    @Test
    fun monthsAreReportedBelowAYear() {
        assertEquals(ChronoUnit.MONTHS to 1L, unitFor(now.minusMonths(1)))
        assertEquals(ChronoUnit.MONTHS to 2L, unitFor(now.minusMonths(2)))
        assertEquals(ChronoUnit.MONTHS to 11L, unitFor(now.minusMonths(11)))
    }

    @Test
    fun yearsAreReportedFromTwelveMonths() {
        assertEquals(ChronoUnit.YEARS to 1L, unitFor(now.minusMonths(12)))
        assertEquals(ChronoUnit.YEARS to 1L, unitFor(now.minusMonths(23)))
        assertEquals(ChronoUnit.YEARS to 2L, unitFor(now.minusYears(2)))
    }
}
