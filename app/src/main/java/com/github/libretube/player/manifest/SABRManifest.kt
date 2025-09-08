package com.github.libretube.player.manifest

import androidx.media3.common.C
import androidx.media3.common.StreamKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.FilterableManifest

/**
 * Represents a SABR streaming manifest.
 */
@UnstableApi
class SABRManifest private constructor(
    /**
     * The overall presentation duration of the media in microseconds, or [C.TIME_UNSET] if the
     * duration is unknown.
     */
    val durationUs: Long,
) : FilterableManifest<SABRManifest?> {

    override fun copy(streamKeys: List<StreamKey>): SABRManifest {
        TODO("Not yet implemented")
    }
}