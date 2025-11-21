package com.github.libretube.player

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.C.TrackType
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.chunk.BaseMediaChunkIterator
import androidx.media3.exoplayer.source.chunk.BundledChunkExtractor
import androidx.media3.exoplayer.source.chunk.Chunk
import androidx.media3.exoplayer.source.chunk.ChunkExtractor
import androidx.media3.exoplayer.source.chunk.ChunkHolder
import androidx.media3.exoplayer.source.chunk.ContainerMediaChunk
import androidx.media3.exoplayer.source.chunk.InitializationChunk
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.CmcdConfiguration
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.FallbackOptions
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import androidx.media3.extractor.ChunkIndex
import com.github.libretube.player.manifest.Representation
import com.github.libretube.player.manifest.SabrManifest
import com.github.libretube.player.parser.SabrClient

/** A default [SabrChunkSource] implementation.  */
@UnstableApi
class DefaultSabrChunkSource(
    chunkExtractorFactory: ChunkExtractor.Factory,
    private val manifest: SabrManifest,
    private val adaptationSetIndices: IntArray,
    private var trackSelection: ExoTrackSelection,
    private val trackType: @TrackType Int,
    private val dataSource: DataSource,
    private val playerId: PlayerId,
) : SabrChunkSource {

    /** [SabrChunkSource.Factory] for [DefaultSabrChunkSource] instances.  */
    class Factory(
        private val dataSourceFactory: DataSource.Factory,
    ) : SabrChunkSource.Factory {
        private val chunkExtractorFactory = BundledChunkExtractor.Factory()

        override fun createSabrChunkSource(
            manifest: SabrManifest,
            adaptationSetIndices: IntArray,
            trackSelection: ExoTrackSelection,
            trackType: @TrackType Int,
            elapsedRealtimeOffsetMs: Long,
            transferListener: TransferListener?,
            playerId: PlayerId,
            cmcdConfiguration: CmcdConfiguration?,
        ): SabrChunkSource? {
            val dataSource = dataSourceFactory.createDataSource()
            transferListener?.let { dataSource.addTransferListener(it) }
            return DefaultSabrChunkSource(
                chunkExtractorFactory,
                manifest,
                adaptationSetIndices,
                trackSelection,
                trackType,
                dataSource,
                playerId,
            )
        }

        /**
         * {@inheritDoc}
         *
         *
         * This implementation delegates determining of the output format to the [ ] passed to the constructor of this class.
         */
        override fun getOutputTextFormat(sourceFormat: Format): Format {
            return chunkExtractorFactory.getOutputTextFormat(sourceFormat)
        }
    }

    private val representationHolders: MutableList<RepresentationHolder>

    private var fatalError: Exception? = null
    private var missingLastSegment = false

    /**
     * @param chunkExtractorFactory Creates [ChunkExtractor] instances to use for extracting
     * chunks.
     * @param manifest The initial manifest.
     * @param adaptationSetIndices The indices of the adaptation sets in the period.
     * @param trackSelection The track selection.
     * @param trackType The [type][C.TrackType] of the tracks in the selection.
     * @param dataSource A [DataSource] suitable for loading the media data.
     * @param playerId The [PlayerId] of the player using this chunk source.
     */
    init {
        val representations =
            adaptationSetIndices.flatMap { manifest.adaptationSets[it].representations }
                .filterNotNull().toList()
        representationHolders =
        (0..trackSelection.length() - 1).map {
            val representation = representations[trackSelection.getIndexInTrackGroup(it)]
                RepresentationHolder(
                    Util.msToUs(manifest.durationMs),
                    representation,
                    chunkExtractorFactory.createProgressiveMediaExtractor(
                        trackType,
                        representation.format,
                        false,
                        emptyList(),
                        null,
                        playerId
                    ),
                )
        }.toMutableList()
    }

    override fun getAdjustedSeekPositionUs(positionUs: Long, seekParameters: SeekParameters): Long {
        // Segments are aligned across representations, so any segment index will do.
        for (representationHolder in representationHolders) {
            if (representationHolder.chunkIndex != null) {
                val segmentCount = representationHolder.segmentCount
                if (segmentCount == 0L) {
                    continue
                }
                val segmentNum = representationHolder.getSegmentNum(positionUs)
                val firstSyncUs = representationHolder.getSegmentStartTimeUs(segmentNum)
                val secondSyncUs = if (firstSyncUs < positionUs &&
                    (segmentCount == RepresentationHolder.INDEX_UNBOUNDED || (segmentNum < segmentCount - 1))
                )
                    representationHolder.getSegmentStartTimeUs(segmentNum + 1)
                else firstSyncUs
                return seekParameters.resolveSeekPositionUs(positionUs, firstSyncUs, secondSyncUs)
            }
        }
        // We don't have a segment index to adjust the seek position with yet.
        return positionUs
    }

    override fun updateTrackSelection(trackSelection: ExoTrackSelection?) {
        this.trackSelection = trackSelection!!
    }

    override fun maybeThrowError() {
        if (fatalError != null) {
            throw fatalError!!
        }
    }

    override fun getPreferredQueueSize(
        playbackPositionUs: Long,
        queue: MutableList<out MediaChunk>,
    ): Int {
        if (fatalError != null || trackSelection.length() < 2) {
            return queue.size
        }
        return trackSelection.evaluateQueueSize(playbackPositionUs, queue)
    }

    override fun shouldCancelLoad(
        playbackPositionUs: Long, loadingChunk: Chunk, queue: MutableList<out MediaChunk>,
    ): Boolean {
        if (fatalError != null) {
            return false
        }
        return trackSelection.shouldCancelChunkLoad(playbackPositionUs, loadingChunk, queue)
    }

    override fun getNextChunk(
        loadingInfo: LoadingInfo,
        loadPositionUs: Long,
        queue: List<MediaChunk>,
        out: ChunkHolder,
    ) {
        if (fatalError != null) {
            return
        }

        val playbackPositionUs = loadingInfo.playbackPositionUs
        val bufferedDurationUs = loadPositionUs - playbackPositionUs

        val previousChunk = queue.lastOrNull()

        val chunkIterators = representationHolders.map {
            if (it.chunkIndex == null) MediaChunkIterator.EMPTY
            else {
                val lastAvailableSegmentNum= it.getLastAvailableSegmentNum()

                val segmentNum = previousChunk?.nextChunkIndex ?: Util.constrainValue(
                    it.getSegmentNum(loadPositionUs),
                    0,
                    lastAvailableSegmentNum
                )

                RepresentationSegmentIterator(
                    it, segmentNum, lastAvailableSegmentNum
                )
            }
        }.toTypedArray()

        trackSelection.updateSelectedTrack(
            playbackPositionUs,
            bufferedDurationUs,
            C.TIME_UNSET,
            queue,
            chunkIterators,
        )

        val representationHolder = representationHolders[trackSelection.selectedIndex]

        if (representationHolder.chunkExtractor != null) {
            if (representationHolder.chunkExtractor.sampleFormats == null || representationHolder.chunkIndex == null) {
                val dataSpec = DataSpec.Builder()
                        // must be non-null, but is unused
                        .setUri(manifest.serverAbrStreamingUri)
                        //TODO: pass along additional data
                        .setCustomData(representationHolder.representation.formatId())
                        .build()
                // when we request a new format, it should start with a initialization chunk (at least I hope so)
                out.chunk = InitializationChunk(
                    dataSource,
                    dataSpec,
                    trackSelection.selectedFormat,
                    trackSelection.selectionReason,
                    trackSelection.selectionData,
                    representationHolder.chunkExtractor
                )
                return
            }
        }

        if (representationHolder.segmentCount == 0L) {
            // The index doesn't define any segments.
            out.endOfStream = true;
            return;
        }

        val lastAvailableSegmentNum = representationHolder.getLastAvailableSegmentNum()
        val segmentNum = previousChunk?.nextChunkIndex ?: Util.constrainValue(
            representationHolder.getSegmentNum(loadPositionUs),
            0,
            lastAvailableSegmentNum
        )

        //TODO: is this check needed?
        if (segmentNum > lastAvailableSegmentNum
            || (missingLastSegment && segmentNum >= lastAvailableSegmentNum)) {
          // The segment is beyond the end of the period.
          out.endOfStream = true;
          return;
        }

        //TODO: is this check needed?
        if (representationHolder.getSegmentStartTimeUs(segmentNum) >= Util.msToUs(manifest.durationMs)) {
          // The period duration clips the period to a position before the segment.
          out.endOfStream = true;
          return;
        }

        val seekTimeUs = if (queue.isEmpty()) loadPositionUs else C.TIME_UNSET
        val dataSpec = DataSpec.Builder().setUri(manifest.serverAbrStreamingUri).setCustomData(
            representationHolder.representation.formatId()
        ).build()

        val startTimeUs = representationHolder.getSegmentStartTimeUs(segmentNum)

        out.chunk = ContainerMediaChunk(
            dataSource,
            dataSpec,
            trackSelection.selectedFormat,
            trackSelection.selectionReason,
            trackSelection.selectionData,
            startTimeUs,
            representationHolder.getSegmentEndTimeUs(segmentNum),
            seekTimeUs,
            representationHolder.periodDurationUs,
            segmentNum,
            1,
            0,
            representationHolder.chunkExtractor!!
        )
    }

    override fun onChunkLoadCompleted(chunk: Chunk) {
        if (chunk is InitializationChunk) {
            val trackIndex = trackSelection.indexOf(chunk.trackFormat)
            val representationHolder = representationHolders[trackIndex]
            // The null check avoids overwriting an index obtained from the manifest with one obtained
            // from the stream. If the manifest defines an index then the stream shouldn't, but in cases
            // where it does we should ignore it.
            if (representationHolder.chunkIndex == null) {
                representationHolder.chunkExtractor?.chunkIndex?.let {
                    representationHolders[trackIndex].chunkIndex = it
                }
            }
        }
    }

    override fun onChunkLoadError(
        chunk: Chunk,
        cancelable: Boolean,
        loadErrorInfo: LoadErrorInfo,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): Boolean {
        if (!cancelable) {
            return false
        }
        // Workaround for missing segment at the end of the period
        if (chunk is MediaChunk
            && loadErrorInfo.exception is InvalidResponseCodeException
            && (loadErrorInfo.exception as InvalidResponseCodeException).responseCode == 404
        ) {
            val representationHolder =
                representationHolders[trackSelection.indexOf(chunk.trackFormat)]
            val segmentCount = representationHolder.segmentCount
            if (segmentCount != RepresentationHolder.INDEX_UNBOUNDED && segmentCount != 0L) {
                val lastAvailableSegmentNum = segmentCount - 1
                if (chunk.nextChunkIndex > lastAvailableSegmentNum) {
                    missingLastSegment = true
                    return true
                }
            }
        }

        val fallbackOptions = createFallbackOptions(trackSelection)
        if (!fallbackOptions.isFallbackAvailable(LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK)
            && !fallbackOptions.isFallbackAvailable(LoadErrorHandlingPolicy.FALLBACK_TYPE_LOCATION)
        ) {
            return false
        }
        val fallbackSelection =
            loadErrorHandlingPolicy.getFallbackSelectionFor(fallbackOptions, loadErrorInfo)
        if (fallbackSelection == null || !fallbackOptions.isFallbackAvailable(fallbackSelection.type)) {
            // Policy indicated to not use any fallback or a fallback type that is not available.
            return false
        }

        var cancelLoad = false
        if (fallbackSelection.type == LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK) {
            cancelLoad =
                trackSelection.excludeTrack(
                    trackSelection.indexOf(chunk.trackFormat), fallbackSelection.exclusionDurationMs
                )
        }
        return cancelLoad
    }

    override fun release() {
        for (representationHolder in representationHolders) {
            representationHolder.chunkExtractor?.release()
        }
    }

    private fun createFallbackOptions(trackSelection: ExoTrackSelection): FallbackOptions {
        val nowMs = SystemClock.elapsedRealtime()
        val numberOfTracks = trackSelection.length()
        var numberOfExcludedTracks = 0
        for (i in 0..<numberOfTracks) {
            if (trackSelection.isTrackExcluded(i, nowMs)) {
                numberOfExcludedTracks++
            }
        }
        return FallbackOptions(
            0,
            0,
            numberOfTracks,
            numberOfExcludedTracks
        )
    }

    /** [MediaChunkIterator] wrapping a [RepresentationHolder].  */
    class RepresentationSegmentIterator
    /**
     * Creates iterator.
     *
     * @param representationHolder The [RepresentationHolder] to wrap.
     * @param firstAvailableSegmentNum The number of the first available segment.
     * @param lastAvailableSegmentNum The number of the last available segment.
     */(
        private val representationHolder: RepresentationHolder,
        firstAvailableSegmentNum: Long,
        lastAvailableSegmentNum: Long,
    ) : BaseMediaChunkIterator(firstAvailableSegmentNum, lastAvailableSegmentNum) {
        override fun getDataSpec(): DataSpec {
            checkInBounds()
            val dataSpec = DataSpec.Builder()
                // must be non-null, but is unused
                .setUri(SabrClient.url)
                //TODO: pass along additional data
                .setCustomData(representationHolder.representation.formatId())
                .build()

            return dataSpec
        }

        override fun getChunkStartTimeUs(): Long {
            checkInBounds()
            return representationHolder.getSegmentStartTimeUs(currentIndex)
        }

        override fun getChunkEndTimeUs(): Long {
            checkInBounds()
            return representationHolder.getSegmentEndTimeUs(currentIndex)
        }
    }

    /** Holds information about a snapshot of a single [Representation].  */
    data class RepresentationHolder(
        val periodDurationUs: Long,
        val representation: Representation,
        val chunkExtractor: ChunkExtractor?,
    ) {
        var chunkIndex: ChunkIndex? = null

        val segmentCount: Long
            get() = chunkIndex?.length?.toLong() ?: 0

        fun getSegmentStartTimeUs(segmentNum: Long): Long = chunkIndex!!.timesUs[segmentNum.toInt()]

        fun getSegmentEndTimeUs(segmentNum: Long): Long =
            (getSegmentStartTimeUs(segmentNum) + chunkIndex!!.durationsUs[segmentNum.toInt()])

        fun getSegmentNum(positionUs: Long): Long =
            chunkIndex!!.getChunkIndex(positionUs).toLong()

        fun getLastAvailableSegmentNum(): Long = chunkIndex!!.length.toLong() - 1

        companion object {
            const val INDEX_UNBOUNDED = -1L
        }
    }
}
