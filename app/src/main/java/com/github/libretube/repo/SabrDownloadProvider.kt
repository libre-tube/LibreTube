package com.github.libretube.repo

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.player.manifest.Representation
import com.github.libretube.player.manifest.SabrManifest
import com.github.libretube.player.parser.PlaybackRequest
import com.github.libretube.player.parser.SabrClient
import com.github.libretube.player.parser.Segment
import okio.BufferedSink
import kotlin.io.path.fileSize

data class SabrDownloaderHandle(
    val sabrClient: SabrClient,
    @SuppressLint("UnsafeOptInUsageError")
    val streamRepresentation: Representation,
    var initSegment: Segment? = null,
    var nextSegmentNumber: Long = 0L
)

@OptIn(UnstableApi::class)
class SabrDownloadProvider(
    downloadItem: DownloadItem,
    streams: Streams,
    stream: PipedStream,
) : DownloadProvider {
    private val downloadHandle: SabrDownloaderHandle

    init {
        val sabrManifest = SabrManifest(downloadItem.videoId, streams)
        val sabrClient = SabrClient(sabrManifest)

        val streamRepresentation = Representation(stream)
        sabrClient.selectFormat(streamRepresentation)

        downloadHandle = SabrDownloaderHandle(sabrClient, streamRepresentation)
    }

    override suspend fun downloadNextChunk(
        item: DownloadItem,
        sink: BufferedSink,
    ): DownloadProgressResult {
        var currentPositionMillis = item.currentDownloadPositionMillis ?: 0L

        if (downloadHandle.initSegment == null) {
            val initRequest = PlaybackRequest.initRequest(
                format = downloadHandle.streamRepresentation.formatId(),
                playerPosition = currentPositionMillis,
                playbackSpeed = 1f
            )
            val initSegment = downloadHandle.sabrClient
                .getNextSegment(initRequest) ?: return DownloadProgressResult.Failed
            downloadHandle.initSegment = initSegment

            val resumeSegment = item.currentSegmentNumber
            if (item.path.fileSize() > 0L && resumeSegment != null) {
                // Existing file already contains the init segment; keep fetching from the stored index.
                downloadHandle.nextSegmentNumber = resumeSegment
            } else {
                for (chunk in initSegment.data) {
                    sink.write(chunk)
                }
                sink.emit()
                downloadHandle.nextSegmentNumber = initSegment.sequenceNumber + 1
            }
        }

        val request = PlaybackRequest(
            format = downloadHandle.streamRepresentation.formatId(),
            playerPosition = currentPositionMillis,
            segment = downloadHandle.nextSegmentNumber,
            segmentStartTimeMs = currentPositionMillis,
            playbackSpeed = 1f,
            bufferedSegments = emptyList()
        )
        val segment = downloadHandle.sabrClient.getNextSegment(request)
            ?: return DownloadProgressResult.Failed

        for (chunk in segment.data) {
            sink.write(chunk)
        }
        sink.emit()

        downloadHandle.nextSegmentNumber = segment.sequenceNumber + 1
        currentPositionMillis += segment.duration

        // persist current download position in millis in the database
        // this is used to restore the download position when pausing and resuming the download
        item.currentDownloadPositionMillis = currentPositionMillis
        item.currentSegmentNumber = downloadHandle.nextSegmentNumber
        DatabaseHolder.Database.downloadDao().updateDownloadItem(item)

        val downloadedBytesLength = segment.data.sumOf { it.size }.toLong()
        val endSegmentNumber = downloadHandle.sabrClient.getEndSegmentNumber(
            downloadHandle.streamRepresentation.formatId()
        )
        // end_segment_number is the last media segment index (inclusive). Do not complete
        // just because the server has not sent initialization metadata yet.
        return if (endSegmentNumber == null || downloadHandle.nextSegmentNumber <= endSegmentNumber) {
            DownloadProgressResult.Progressed(downloadedBytesLength)
        } else {
            DownloadProgressResult.DownloadComplete
        }
    }
}