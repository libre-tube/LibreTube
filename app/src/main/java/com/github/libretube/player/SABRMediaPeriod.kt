package com.github.libretube.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.C.TrackType
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.StreamKey
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory
import androidx.media3.exoplayer.source.DefaultCompositeSequenceableLoaderFactory
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleQueue
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.source.SequenceableLoader
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.poToken.PoTokenGenerator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(UnstableApi::class)
class SABRMediaPeriod(
    private val uri: Uri,
    private val allocator: Allocator,
    private val videoId: String,
    private val streams: Streams,
) : MediaPeriod {
    private val TAG = SABRMediaPeriod::class.simpleName

    private val sabrStream: SABRStream = SABRStream()
    private val tracks: Array<TrackGroup> = buildTracks()
    private val compositeSequenceableLoaderFactory: CompositeSequenceableLoaderFactory =
        DefaultCompositeSequenceableLoaderFactory()
    var compositeSequenceableLoader = compositeSequenceableLoaderFactory.empty()

    @kotlin.OptIn(ExperimentalEncodingApi::class)
    override fun prepare(
        callback: MediaPeriod.Callback,
        positionUs: Long
    ) {
        Log.e(TAG, "prepare: ${streams.videoPlaybackUstreamerConfig}")
        sabrStream.prepare(
            videoId,
            uri.toString(),
            Base64.UrlSafe.decode(streams.videoPlaybackUstreamerConfig!!),
            //TODO: retrieve the poToken from the generator
            byteArrayOf(),
            //Base64.UrlSafe.decode(PoTokenGenerator().getWebEmbedClientPoToken(videoId)?.streamingDataPoToken!!),
            //TODO: retrieve the appropriate itag from the player
            streams.audioStreams.last().itag!!,
            streams.audioStreams.last().lastModified!!,
            streams.videoStreams.first().itag!!,
            streams.videoStreams.first().lastModified!!,
        )

        callback.onPrepared(this)
    }

    override fun maybeThrowPrepareError() {}

    override fun getTrackGroups(): TrackGroupArray = TrackGroupArray(*this.tracks)

    @Suppress("UNCHECKED_CAST")
    override fun selectTracks(
        selections: Array<out ExoTrackSelection?>,
        mayRetainStreamFlags: BooleanArray,
        streams: Array<out SampleStream?>,
        streamResetFlags: BooleanArray,
        positionUs: Long
    ): Long {
        // stupid hack because kotlin doesn't allow assigning null in an out array
        val streams = streams as Array<SampleStream?>


        // deselect old tracks.
        for (i in 0..<selections.size) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                streams[i] = null
            }
        }

       //TODO: select new tracks

        return positionUs
    }


    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {}

    override fun readDiscontinuity(): Long = C.TIME_UNSET

    override fun seekToUs(positionUs: Long): Long = positionUs

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters
    ): Long = positionUs

    override fun getBufferedPositionUs(): Long = compositeSequenceableLoader.bufferedPositionUs

    override fun getNextLoadPositionUs(): Long = compositeSequenceableLoader.nextLoadPositionUs

    override fun continueLoading(loadingInfo: LoadingInfo): Boolean =
        compositeSequenceableLoader.continueLoading(loadingInfo)

    override fun isLoading(): Boolean = compositeSequenceableLoader.isLoading

    override fun reevaluateBuffer(positionUs: Long) =
        compositeSequenceableLoader.reevaluateBuffer(positionUs)

    fun release() {
        sabrStream.destroy()
    }

    private fun buildTracks(): Array<TrackGroup> {
        val tracks = mutableListOf<TrackGroup>()
        val audioFormats = streams.audioStreams.take(1).map { stream ->
            Format.Builder()
                .setId(stream.itag?: 0)
                .setCodecs(stream.codec)
                .setContainerMimeType(stream.mimeType)
                .setSampleMimeType(MimeTypes.getAudioMediaMimeType(stream.codec))
                .setAverageBitrate(stream.bitrate ?: -1)
                .setChannelCount(2)
                //TODO: group audio tracks by locale
                .setLanguage(stream.audioTrackLocale)
                .build()
        }.toTypedArray()
        tracks.add(TrackGroup("audio", *audioFormats))
        sampleStreams.put("audio", SABRSampleStream(allocator, sabrStream, C.TRACK_TYPE_AUDIO, audioFormats.first()))

        val videoFormats = streams.videoStreams.take(1).map { stream ->
            Format.Builder()
                .setId(stream.itag ?: 0)
                .setCodecs(stream.codec)
                .setContainerMimeType(stream.mimeType)
                .setSampleMimeType(MimeTypes.getVideoMediaMimeType(stream.codec))
                .setAverageBitrate(stream.bitrate ?: -1)
                .setFrameRate(stream.fps?.toFloat() ?: -1f)
                .setWidth(stream.width ?: -1)
                .setHeight(stream.height ?: -1).build()
        }.toTypedArray()
        tracks.add(TrackGroup("video", *videoFormats))
        sampleStreams.put("video", SABRSampleStream(allocator, sabrStream, C.TRACK_TYPE_VIDEO, videoFormats.first()))

        return tracks.toTypedArray()
    }
}
