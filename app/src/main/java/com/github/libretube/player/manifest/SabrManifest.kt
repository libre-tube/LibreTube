package com.github.libretube.player.manifest

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.StreamKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.FilterableManifest
import com.github.libretube.api.obj.Streams

/**
 * Represents server adaptive-bitrate streaming media metadata.
 */
@UnstableApi
class SabrManifest(
    /**
     * Identifier of the video being streamed.
     */
    val videoId: String,
    /**
     * URL of the streaming server.
     */
    val serverAbrStreamingUri: Uri,
    /**
     * Required config for media playback.
     */
    val videoPlaybackUstreamerConfig: ByteArray,
    /**
     * The duration of the presentation in milliseconds, or [C.TIME_UNSET] if not applicable.
     */
    val durationMs: Long,
) : FilterableManifest<SabrManifest?> {
    var adaptationSets: List<AdaptationSet> = emptyList()
        private set

    internal constructor(
        videoId: String,
        streams: Streams
    ) : this(
        videoId,
        streams.serverAbrStreamingUrl!!.toUri(),
        Base64.decode(streams.videoPlaybackUstreamerConfig!!, Base64.URL_SAFE),
        streams.duration * 1000,
    ) {
        val videoAdaptionSets = streams.videoStreams.groupBy { it.mimeType }
            .map { (_, streams) ->
                AdaptationSet(C.TRACK_TYPE_VIDEO, streams.map {
                    Representation(it)
                })
            };

        val audioAdaptationSets = streams.audioStreams.groupBy { it.mimeType + it.audioTrackId }
            .map { (_, streams) ->
                AdaptationSet(C.TRACK_TYPE_AUDIO, streams.map {
                    Representation(it)
                })
            }
        adaptationSets = videoAdaptionSets + audioAdaptationSets
    }

    override fun copy(streamKeys: List<StreamKey>): SabrManifest {
        return this
    }
}
