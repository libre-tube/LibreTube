package com.github.libretube.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.chunk.Chunk
import androidx.media3.exoplayer.source.chunk.ChunkHolder
import androidx.media3.exoplayer.source.chunk.ContainerMediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.TrackSelectionUtil
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoaderErrorThrower
import com.github.libretube.player.manifest.SABRManifest

@OptIn(UnstableApi::class)
class DefaultSABRChunkSource(
    private var trackSelection: ExoTrackSelection,
    private var dataSourceFactory: SABRDataSouce.Factory,
) : SABRChunkSource {

    class Factory(private val dataSourceFactory: SABRDataSouce.Factory) : SABRChunkSource.Factory {
        override fun createChunkSource(
            manifestLoaderErrorThrower: LoaderErrorThrower,
            manifest: SABRManifest,
            streamElementIndex: Int,
            trackSelection: ExoTrackSelection,
        ): SABRChunkSource? = DefaultSABRChunkSource(trackSelection, dataSourceFactory)
    }

    override fun updateManifest(newManifest: SABRManifest) {}

    override fun updateTrackSelection(trackSelection: ExoTrackSelection) {
        this.trackSelection = trackSelection
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters
    ): Long {
        TODO("Not yet implemented")
    }

    override fun maybeThrowError() {}

    override fun getPreferredQueueSize(
        playbackPositionUs: Long,
        queue: List<out MediaChunk>
    ): Int = trackSelection.evaluateQueueSize(playbackPositionUs, queue)

    override fun shouldCancelLoad(
        playbackPositionUs: Long,
        loadingChunk: Chunk,
        queue: List<out MediaChunk>
    ): Boolean = trackSelection.shouldCancelChunkLoad(playbackPositionUs, loadingChunk, queue)

    override fun getNextChunk(
        loadingInfo: LoadingInfo,
        loadPositionUs: Long,
        queue: List<out MediaChunk>,
        out: ChunkHolder
    ) {
        TODO("Not yet implemented")
    }

    override fun onChunkLoadCompleted(chunk: Chunk) {}

    override fun onChunkLoadError(
        chunk: Chunk,
        cancelable: Boolean,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): Boolean {
        val fallbackSelection = loadErrorHandlingPolicy.getFallbackSelectionFor(
            TrackSelectionUtil.createFallbackOptions(trackSelection), loadErrorInfo
        )
        return cancelable && fallbackSelection != null && fallbackSelection.type == LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK
    }

    override fun release() {
        TODO("Not yet implemented")
    }
}