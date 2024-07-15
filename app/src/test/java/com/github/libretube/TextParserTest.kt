package com.github.libretube

import com.github.libretube.util.TextUtils.parseDurationString
import com.github.libretube.util.TextUtils.toTimeInSeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class TextParserTest {
    @Test
    fun testTimeParser() {
        assertEquals(15L * 60 + 20, "15m 20s".toTimeInSeconds())
        assertEquals(1520L, "1520".toTimeInSeconds())
        assertEquals(15L * 60 + 20, "15:20.25".toTimeInSeconds())
        assertEquals(15f * 60 + 20 + 0.25f, "15:20.25".parseDurationString())
        assertEquals(20f, "00:20".parseDurationString())
        assertEquals(60.02503f, "1:00.02503".parseDurationString())
    }
}