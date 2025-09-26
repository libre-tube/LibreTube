package com.github.libretube.player.manifest

import android.net.Uri
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
     * Identifier of the video being streamed.
     */
    val videoId: String,
    /**
     * URL of the streaming server.
     */
    val streamingUri: Uri,
    /**
     * Required config for media playback.
     */
    val videoPlaybackUstreamerConfig: ByteArray,
    /**
     * Proof or Origin token.
     */
    val poToken: ByteArray?,
    /**
     * The overall presentation duration of the media in microseconds, or [C.TIME_UNSET] if the
     * duration is unknown.
     */
    val durationUs: Long,
    val adaptationSets: List<AdaptationSet>,
) : FilterableManifest<SABRManifest?> {

    override fun copy(streamKeys: List<StreamKey>): SABRManifest {
        TODO("Not yet implemented")
    }
}