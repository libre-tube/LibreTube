package com.github.libretube.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.chunk.Chunk
import androidx.media3.exoplayer.source.chunk.ChunkExtractor
import androidx.media3.exoplayer.source.chunk.ChunkHolder
import androidx.media3.exoplayer.source.chunk.ContainerMediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.TrackSelectionUtil
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoaderErrorThrower
import com.github.libretube.player.manifest.Representation
import com.github.libretube.player.manifest.SABRManifest


// Adapted from https://github.com/androidx/media/blob/bfe5930f7f29c6492d60e3d01a90abd3c138b615/libraries/exoplayer_dash/src/main/java/androidx/media3/exoplayer/dash/DefaultDashChunkSource.java#L195
// Licensed under Apache 2.0

@OptIn(UnstableApi::class)
class DefaultSABRChunkSource(
    private var manifest: SABRManifest,
    private val trackType: C.TrackType,
    private val adaptationSetIndices: IntArray,
    private var trackSelection: ExoTrackSelection,
    private var dataSourceFactory: SABRDataSouce.Factory,
    private var chunkExtractorFactory: ChunkExtractor.Factory,
    private var playerId: PlayerId,
) : SABRChunkSource {
    private lateinit var representationHolders: List<RepresentationHolder>

    init {
        val representations = getRepresentations()
        List(
            trackSelection.length(),
            {
                RepresentationHolder(
                    representations[it]!!,
                    chunkExtractorFactory.createProgressiveMediaExtractor(
                        trackType as Int,
                        representations[it]!!.format,
                        false,
                        emptyList(),
                        null,
                        playerId,
                    ),
                    0
                )
            })

    }

    class Factory(
        private val dataSourceFactory: SABRDataSouce.Factory,
        private val chunkExtractorFactory: ChunkExtractor.Factory,
    ) : SABRChunkSource.Factory {
        override fun createChunkSource(
            manifestLoaderErrorThrower: LoaderErrorThrower,
            manifest: SABRManifest,
            streamElementIndex: Int,
            trackSelection: ExoTrackSelection,
            playerId: PlayerId,
        ): SABRChunkSource? =
            DefaultSABRChunkSource(
                manifest,
                C.TRACK_TYPE_VIDEO as C.TrackType,
                intArrayOf(),
                trackSelection,
                dataSourceFactory,
                chunkExtractorFactory,
                playerId,
            )
    }

    override fun updateManifest(newManifest: SABRManifest) {}

    override fun updateTrackSelection(trackSelection: ExoTrackSelection) {
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters,
    ): Long {
        TODO("Not yet implemented")
    }

    override fun maybeThrowError() {}

    override fun getPreferredQueueSize(
        playbackPositionUs: Long,
        queue: List<out MediaChunk>,
    ): Int = trackSelection.evaluateQueueSize(playbackPositionUs, queue)

    override fun shouldCancelLoad(
        playbackPositionUs: Long,
        loadingChunk: Chunk,
        queue: List<out MediaChunk>,
    ): Boolean = trackSelection.shouldCancelChunkLoad(playbackPositionUs, loadingChunk, queue)

    override fun getNextChunk(
        loadingInfo: LoadingInfo,
        loadPositionUs: Long,
        queue: List<out MediaChunk>,
        out: ChunkHolder,
    ) {
        val playbackPositionUs = loadingInfo.playbackPositionUs
        val bufferedDurationUs = loadPositionUs - playbackPositionUs
        val presentationPositionUs =
            (Util.msToUs(manifest.availabilityStartTimeMs)
                    + Util.msToUs(manifest.getPeriod(periodIndex).startMs)
                    + loadPositionUs)
        val previousChunk = queue.lastOrNull()

        out.chunk = ContainerMediaChunk(
            dataSourceFactory.createDataSource(),
            DataSpec(Uri.Builder().scheme("sabr").build()),
            trackSelection.selectedFormat,
            trackSelection.selectionReason,
        )
        TODO("Not yet implemented")
    }

    override fun onChunkLoadCompleted(chunk: Chunk) {}

    override fun onChunkLoadError(
        chunk: Chunk,
        cancelable: Boolean,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): Boolean {
        val fallbackSelection = loadErrorHandlingPolicy.getFallbackSelectionFor(
            TrackSelectionUtil.createFallbackOptions(trackSelection), loadErrorInfo
        )
        return cancelable && fallbackSelection != null && fallbackSelection.type == LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK
    }

    override fun release() {
        TODO("Not yet implemented")
    }

    private fun getRepresentations(
    ): List<Representation?> = adaptationSetIndices.flatMap { index ->
        manifest.adaptationSets[index]?.representations?.toList()!!
    }

    /** Holds information about a snapshot of a single [Representation].  */
    private class RepresentationHolder(
        private val representation: Representation,
        private val chunkExtractor: ChunkExtractor?,
        private val segmentIndex: Int,
    ) {}
}