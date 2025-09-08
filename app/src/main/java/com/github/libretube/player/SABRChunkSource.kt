package com.github.libretube.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.chunk.ChunkSource
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.CmcdConfiguration
import androidx.media3.exoplayer.upstream.LoaderErrorThrower
import com.github.libretube.player.manifest.SABRManifest

/** A [ChunkSource] for SABR streams.  */
@UnstableApi
interface SABRChunkSource : ChunkSource {
    interface Factory {
        /**
         * Creates a new [SABRChunkSource].
         *
         * @param manifestLoaderErrorThrower Throws errors affecting loading of manifests.
         * @param manifest The initial manifest.
         * @param streamElementIndex The index of the corresponding stream element in the manifest.
         * @param trackSelection The track selection.
         * @return The created [SABRChunkSource].
         */
        fun createChunkSource(
            manifestLoaderErrorThrower: LoaderErrorThrower,
            manifest: SABRManifest,
            streamElementIndex: Int,
            trackSelection: ExoTrackSelection,
        ): SABRChunkSource?
    }

    /**
     * Updates the manifest.
     *
     * @param newManifest The new manifest.
     */
    fun updateManifest(newManifest: SABRManifest)

    /**
     * Updates the track selection.
     *
     * @param trackSelection The new track selection instance. Must be equivalent to the previous one.
     */
    fun updateTrackSelection(trackSelection: ExoTrackSelection)
}