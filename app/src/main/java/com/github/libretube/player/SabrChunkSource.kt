package com.github.libretube.player

import androidx.media3.common.C.TrackType
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.chunk.ChunkSource
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.CmcdConfiguration
import com.github.libretube.player.manifest.SabrManifest

/** A [ChunkSource] for Sabr streams.  */
@UnstableApi
interface SabrChunkSource : ChunkSource {
    /** Factory for [SabrChunkSource]s.  */
    interface Factory {
        /**
         * @param manifest The initial manifest.
         * @param periodIndex The index of the corresponding period in the manifest.
         * @param adaptationSetIndices The indices of the corresponding adaptation sets in the period.
         * @param trackSelection The track selection.
         * @param trackType The [track type][androidx.media3.common.C.TrackType].
         * @param elapsedRealtimeOffsetMs If known, an estimate of the instantaneous difference between
         * server-side unix time and [android.os.SystemClock.elapsedRealtime] in milliseconds,
         * specified as the server's unix time minus the local elapsed time. Or [androidx.media3.common.C.TIME_UNSET]
         * if unknown.
         * @param transferListener The transfer listener which should be informed of any data transfers.
         * May be null if no listener is available.
         * @param playerId The [PlayerId] of the player using this chunk source.
         * @param cmcdConfiguration The [CmcdConfiguration] for this chunk source.
         * @return The created [SabrChunkSource].
         */
        fun createSabrChunkSource(
            manifest: SabrManifest,
            adaptationSetIndices: IntArray,
            trackSelection: ExoTrackSelection,
            trackType: @TrackType Int,
            elapsedRealtimeOffsetMs: Long,
            transferListener: TransferListener?,
            playerId: PlayerId,
            cmcdConfiguration: CmcdConfiguration?
        ): SabrChunkSource?

        /**
         * Returns the output [Format] of emitted [text samples][androidx.media3.common.C.TRACK_TYPE_TEXT]
         * which were originally in `sourceFormat`.
         *
         * In many cases, where an [androidx.media3.extractor.Extractor] emits samples from the source without mutation,
         * this method simply returns `sourceFormat`. In other cases, such as an [androidx.media3.extractor.Extractor]
         * that transcodes subtitles from the `sourceFormat` to [ ][androidx.media3.common.MimeTypes.APPLICATION_MEDIA3_CUES], the format is updated to indicate the transcoding that is
         * taking place.
         *
         * Non-text source formats are always returned without mutation.
         *
         * @param sourceFormat The original text-based format.
         * @return The [Format] that will be associated with a [text][androidx.media3.common.C.TRACK_TYPE_TEXT].
         */
        fun getOutputTextFormat(sourceFormat: Format): Format? {
            return sourceFormat
        }
    }

    /**
     * Updates the track selection.
     *
     * @param trackSelection The new track selection instance. Must be equivalent to the previous one.
     */
    fun updateTrackSelection(trackSelection: ExoTrackSelection?)
}
