package com.github.libretube.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.drm.DrmSessionEventListener
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.source.CompositeSequenceableLoaderFactory
import androidx.media3.exoplayer.source.DefaultCompositeSequenceableLoaderFactory
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSourceEventListener
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.source.SequenceableLoader
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.source.chunk.ChunkSampleStream
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoaderErrorThrower
import com.github.libretube.api.obj.Streams
import com.github.libretube.player.manifest.SABRManifest
import okhttp3.internal.immutableListOf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(UnstableApi::class)
class SABRMediaPeriod(
    private val uri: Uri,
    private val allocator: Allocator,
    private val videoId: String,
    private val streams: Streams,
    private val manifestLoaderErrorThrower: LoaderErrorThrower,
    private val chunkSourceFactory: SABRChunkSource.Factory,
    private val manifest: SABRManifest,
    private val drmSessionManager: DrmSessionManager,
    private val drmEventDispatcher: DrmSessionEventListener.EventDispatcher,
    private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    private val mediaSourceEventDispatcher: MediaSourceEventListener.EventDispatcher,
) : MediaPeriod, SequenceableLoader.Callback<ChunkSampleStream<SABRChunkSource>> {
    private val TAG = SABRMediaPeriod::class.simpleName

    private var sampleStreams: Array<ChunkSampleStream<SABRChunkSource>> = emptyArray()
    private val sabrStream: SABRStream = SABRStream()
    private val tracks: Array<TrackGroup> = buildTracks()
    private val compositeSequenceableLoaderFactory: CompositeSequenceableLoaderFactory =
        DefaultCompositeSequenceableLoaderFactory()
    private var compositeSequenceableLoader = compositeSequenceableLoaderFactory.empty()
    private var callback: MediaPeriod.Callback? = null

    @kotlin.OptIn(ExperimentalEncodingApi::class)
    override fun prepare(
        callback: MediaPeriod.Callback,
        positionUs: Long
    ) {
        this.callback = callback
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

        val sampleStreamsList: MutableList<ChunkSampleStream<SABRChunkSource>> = mutableListOf();
        for (i in 0..<selections.size) {
            if (streams[i] != null) {
                val stream = streams[i] as ChunkSampleStream<SABRChunkSource>;
                if (selections[i] == null || !mayRetainStreamFlags[i]) {
                    stream.release();
                    streams[i] = null;
                } else {
                    stream.getChunkSource().updateTrackSelection(checkNotNull(selections[i]));
                    sampleStreamsList.add(stream);
                }
            }
            if (streams[i] == null && selections[i] != null) {
                val stream = buildSampleStream(selections[i], positionUs);
                sampleStreamsList.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        sampleStreams = sampleStreamsList.toTypedArray()
        compositeSequenceableLoader = compositeSequenceableLoaderFactory.create(
            sampleStreamsList, sampleStreamsList.map { immutableListOf(it.primaryTrackType) })

        return positionUs
    }


    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {
        for (sampleStream in sampleStreams) {
            sampleStream.discardBuffer(positionUs, toKeyframe)
        }
    }

    override fun reevaluateBuffer(positionUs: Long) =
        compositeSequenceableLoader.reevaluateBuffer(positionUs)

    override fun continueLoading(loadingInfo: LoadingInfo): Boolean =
        compositeSequenceableLoader.continueLoading(loadingInfo)

    override fun isLoading(): Boolean = compositeSequenceableLoader.isLoading

    override fun getNextLoadPositionUs(): Long = compositeSequenceableLoader.nextLoadPositionUs

    override fun readDiscontinuity(): Long = C.TIME_UNSET

    override fun getBufferedPositionUs(): Long = compositeSequenceableLoader.bufferedPositionUs

    override fun seekToUs(positionUs: Long): Long {
        for (sampleStream in sampleStreams) {
            sampleStream.seekToUs(positionUs)
        }
        return positionUs
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters
    ): Long {
        for (sampleStream in sampleStreams) {
            if (sampleStream.primaryTrackType == C.TRACK_TYPE_VIDEO) {
                sampleStream.getAdjustedSeekPositionUs(positionUs, seekParameters)
            }
        }
        return positionUs
    }

    fun release() {
        sabrStream.destroy()
        for (sampleStream in sampleStreams) {
            sampleStream.release()
        }
        callback = null;
    }

    override fun onContinueLoadingRequested(source: ChunkSampleStream<SABRChunkSource>) {
        callback!!.onContinueLoadingRequested(this)
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

        return tracks.toTypedArray()
    }

    private fun buildSampleStream(
        selection: ExoTrackSelection?,
        positionUs: Long
    ) : ChunkSampleStream<SABRChunkSource> {
        val streamElementIndex = trackGroups.indexOf(selection!!.trackGroup)
        val chunkSource =
            chunkSourceFactory.createChunkSource(
                manifestLoaderErrorThrower,
                manifest,
                streamElementIndex,
                selection,
            )!!
        return ChunkSampleStream<SABRChunkSource>(
            //TODO: use correct tracktype
            C.TRACK_TYPE_VIDEO,
            null,
            null,
            chunkSource,
            this,
            allocator,
            positionUs,
            drmSessionManager,
            drmEventDispatcher,
            loadErrorHandlingPolicy,
            mediaSourceEventDispatcher,
            false,
            null
        )
    }
}
