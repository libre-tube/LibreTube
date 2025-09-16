package com.github.libretube.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.chunk.Chunk
import androidx.media3.exoplayer.source.chunk.ChunkHolder
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import com.github.libretube.player.manifest.SABRManifest

@OptIn(UnstableApi::class)
class DefaultSABRChunkSource : SABRChunkSource {
    override fun updateManifest(newManifest: SABRManifest?) {}

    override fun updateTrackSelection(trackSelection: ExoTrackSelection?) {
        TODO("Not yet implemented")
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
    ): Int {
        TODO("Not yet implemented")
    }

    override fun shouldCancelLoad(
        playbackPositionUs: Long,
        loadingChunk: Chunk,
        queue: List<out MediaChunk>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getNextChunk(
        loadingInfo: LoadingInfo,
        loadPositionUs: Long,
        queue: List<out MediaChunk>,
        out: ChunkHolder
    ) {
        TODO("Not yet implemented")
    }

    override fun onChunkLoadCompleted(chunk: Chunk) {
        TODO("Not yet implemented")
    }

    override fun onChunkLoadError(
        chunk: Chunk,
        cancelable: Boolean,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }
}