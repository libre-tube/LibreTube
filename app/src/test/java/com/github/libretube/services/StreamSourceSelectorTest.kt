package com.github.libretube.services

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the stream source selection, in particular the fallback from an
 * empty/blank live DASH URL to HLS (see [selectStreamSource]).
 */
class StreamSourceSelectorTest {

    private val validDash = "https://manifest.googlevideo.com/file/index.mpd"
    private val validHls = "https://manifest.googlevideo.com/file/index.m3u8"

    private fun select(
        isLive: Boolean = false,
        hasVideoStreams: Boolean = true,
        sabrAvailable: Boolean = false,
        dashUrl: String? = null,
        hlsUrl: String? = null
    ): StreamSourceType = selectStreamSource(
        isLive = isLive,
        hasVideoStreams = hasVideoStreams,
        sabrAvailable = sabrAvailable,
        dashUrl = dashUrl,
        hlsUrl = hlsUrl
    )

    @Test
    fun `empty live dash falls back to HLS`() {
        assertEquals(StreamSourceType.HLS, select(isLive = true, dashUrl = "", hlsUrl = validHls))
    }

    @Test
    fun `blank live dash falls back to HLS`() {
        assertEquals(StreamSourceType.HLS, select(isLive = true, dashUrl = "   ", hlsUrl = validHls))
    }

    @Test
    fun `null live dash falls back to HLS`() {
        assertEquals(StreamSourceType.HLS, select(isLive = true, dashUrl = null, hlsUrl = validHls))
    }

    @Test
    fun `valid live dash wins over HLS`() {
        assertEquals(StreamSourceType.DASH, select(isLive = true, dashUrl = validDash, hlsUrl = validHls))
    }

    @Test
    fun `live without any manifest returns NONE`() {
        assertEquals(StreamSourceType.NONE, select(isLive = true, dashUrl = "", hlsUrl = null))
    }

    @Test
    fun `vod with video streams keeps DASH`() {
        assertEquals(StreamSourceType.DASH, select(isLive = false, hasVideoStreams = true))
    }

    @Test
    fun `vod without video streams uses HLS`() {
        assertEquals(StreamSourceType.HLS, select(isLive = false, hasVideoStreams = false, hlsUrl = validHls))
    }

    @Test
    fun `vod with no sources returns NONE`() {
        assertEquals(StreamSourceType.NONE, select(isLive = false, hasVideoStreams = false, hlsUrl = ""))
    }

    @Test
    fun `sabr is preferred for vod`() {
        assertEquals(
            StreamSourceType.SABR,
            select(isLive = false, sabrAvailable = true, dashUrl = validDash)
        )
    }

    @Test
    fun `sabr is skipped for livestreams`() {
        assertEquals(
            StreamSourceType.DASH,
            select(isLive = true, sabrAvailable = true, dashUrl = validDash, hlsUrl = validHls)
        )
    }
}