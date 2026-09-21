package com.github.libretube.helpers

import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import com.github.libretube.enums.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDownloadSelectionTest {
    @Test
    fun defaultLanguageDownloadsAudioWithLocale() {
        assertAudioDownload(listOf(audio("en-US")), null, "en-US")
    }

    @Test
    fun unavailableLanguageDownloadsAvailableAudio() {
        assertAudioDownload(listOf(audio("en-US")), "fr", "en-US")
    }

    @Test
    fun requestedLanguageSelectsItsOwnQualityAndFormat() {
        assertAudioDownload(
            listOf(audio("en-US", "128 bits", "M4A"), audio("fr", "64 bits", "WEBMA_OPUS")),
            "fr", "fr", "64 bits", "WEBMA_OPUS"
        )
    }

    @Test
    fun legacyAudioWithoutLocaleStillDownloads() {
        assertAudioDownload(listOf(audio(null)), null, null)
    }

    @Test
    fun disabledAudioDoesNotCreateDownloadItems() {
        val streams = streams(listOf(audio("en-US")))
        val data = PlaylistDownloadSelection.createDownloadData(
            "video", streams, null, null, null, null
        )
        assertTrue(streams.toDownloadItems(data).isEmpty())
    }

    private fun assertAudioDownload(
        audioStreams: List<PipedStream>,
        requestedLanguage: String?,
        expectedLanguage: String?,
        expectedQuality: String = "128 bits",
        expectedFormat: String = "M4A"
    ) {
        val streams = streams(audioStreams)
        val data = PlaylistDownloadSelection.createDownloadData(
            "video", streams, null, 128, requestedLanguage, null
        )
        val items = streams.toDownloadItems(data)
        assertEquals(1, items.size)
        assertEquals(FileType.AUDIO, items.single().type)
        assertEquals(expectedLanguage, items.single().language)
        assertEquals(expectedQuality, items.single().quality)
        assertEquals(expectedFormat, items.single().format)
    }

    private fun audio(
        language: String?,
        quality: String = "128 bits",
        format: String = "M4A"
    ) = PipedStream(
        format = format,
        quality = quality,
        mimeType = "audio/mp4",
        audioTrackLocale = language
    )

    private fun streams(audioStreams: List<PipedStream>) = Streams(
        title = "Test video",
        description = "",
        uploader = "Test uploader",
        uploaderUrl = null,
        thumbnailUrl = "",
        category = "Music",
        uploaderVerified = false,
        duration = 60,
        audioStreams = audioStreams
    )
}
