package com.github.libretube.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure decision logic of [DataSaverAwareBitmapLoader].
 *
 * Mirrors the guard used by ImageHelper.loadImage for image views: in data
 * saver mode, remote (http/https) artwork must only be loaded when already
 * cached; local artwork and non-data-saver mode stay untouched.
 */
class DataSaverBitmapLoaderTest {
    private val remoteUrl = "https://pipedproxy.example.com/vi/dQw4w9WgXcQ/hqdefault.jpg"

    @Test
    fun `data saver on and remote url not cached skips artwork`() {
        assertTrue(
            DataSaverAwareBitmapLoader.shouldSkipArtwork(
                url = remoteUrl,
                dataSaverEnabled = true,
                cached = false,
            ),
        )
    }

    @Test
    fun `data saver on but url cached loads artwork`() {
        assertFalse(
            DataSaverAwareBitmapLoader.shouldSkipArtwork(
                url = remoteUrl,
                dataSaverEnabled = true,
                cached = true,
            ),
        )
    }

    @Test
    fun `data saver off never skips artwork`() {
        assertFalse(
            DataSaverAwareBitmapLoader.shouldSkipArtwork(
                url = remoteUrl,
                dataSaverEnabled = false,
                cached = false,
            ),
        )
    }

    @Test
    fun `local artwork is never skipped in data saver mode`() {
        assertFalse(
            DataSaverAwareBitmapLoader.shouldSkipArtwork(
                url = "content://media/external/images/media/42",
                dataSaverEnabled = true,
                cached = false,
            ),
        )
        assertFalse(
            DataSaverAwareBitmapLoader.shouldSkipArtwork(
                url = "file:///storage/emulated/0/Android/data/libretube/thumbnails/a.webp",
                dataSaverEnabled = true,
                cached = false,
            ),
        )
    }
}
