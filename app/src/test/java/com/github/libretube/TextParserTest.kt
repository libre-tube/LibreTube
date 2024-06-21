package com.github.libretube

import com.github.libretube.util.TextUtils.parseDurationString
import com.github.libretube.util.TextUtils.toTimeInSeconds
import org.junit.Test
import org.junit.Assert.assertEquals

class TextParserTest {
    @Test
    fun testTimeParser() {
        assertEquals(15L * 60 + 20, "15m 20s".toTimeInSeconds())
        assertEquals(1520L, "1520".toTimeInSeconds())
        assertEquals(15L * 60 + 20, "15:20.25".toTimeInSeconds())
        assertEquals(15f * 60 + 20 + 0.25f, "15:20.25".parseDurationString())
    }
}