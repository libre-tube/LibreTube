package com.github.libretube.player.manifest

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.C.ROLE_FLAG_DESCRIBES_VIDEO
import androidx.media3.common.C.ROLE_FLAG_DUB
import androidx.media3.common.C.ROLE_FLAG_MAIN
import androidx.media3.common.C.ROLE_FLAG_SUPPLEMENTARY
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.StreamKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.FilterableManifest
import com.github.libretube.api.obj.PipedStream
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
                    buildRepresentation(
                        it,
                        Format.Builder()
                            .setCodecs(it.codec)
                            .setContainerMimeType(it.mimeType)
                            .setSampleMimeType(MimeTypes.getVideoMediaMimeType(it.codec))
                            .setAverageBitrate(it.bitrate ?: -1)
                            .setFrameRate(it.fps?.toFloat() ?: -1f)
                            .setWidth(it.width ?: -1)
                            .setHeight(it.height ?: -1).build(),
                    )
                })
            };

        val audioAdaptationSets = streams.audioStreams.groupBy { it.mimeType + it.audioTrackId }
            .map { (_, streams) ->
                AdaptationSet(C.TRACK_TYPE_AUDIO, streams.map {
                    buildRepresentation(
                        it,
                        Format.Builder()
                            .setCodecs(it.codec)
                            .setContainerMimeType(it.mimeType)
                            .setSampleMimeType(MimeTypes.getAudioMediaMimeType(it.codec))
                            .setAverageBitrate(it.bitrate ?: -1)
                            .setChannelCount(2)
                            .setLanguage(it.audioTrackId?.substring(0, 2)?: it.audioTrackLocale)
                            .setRoleFlags(
                                when (it.audioTrackType?.lowercase()) {
                                    "descriptive" -> ROLE_FLAG_DESCRIBES_VIDEO
                                    "original" -> ROLE_FLAG_MAIN
                                    "dubbed", "auto-dubbed" -> ROLE_FLAG_DUB
                                    "secondary" -> ROLE_FLAG_SUPPLEMENTARY
                                    else -> 0
                                }
                            )
                            .build()
                    )
                })
            };
        adaptationSets = videoAdaptionSets + audioAdaptationSets
    }

    override fun copy(streamKeys: List<StreamKey>): SabrManifest {
        return this
    }

    companion object {
        private fun buildRepresentation(stream: PipedStream, format: Format) = Representation(
            format,
            stream,
        )
    }
}
