@file:OptIn(UnstableApi::class)

package com.github.libretube.helpers

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toAndroidUri
import java.io.EOFException
import java.nio.file.Path
import kotlin.io.path.fileSize

sealed class MediaVerifyResult {
    data object Ok : MediaVerifyResult()
    data class Corrupt(val reason: String) : MediaVerifyResult()
}

/**
 * Checks that a downloaded media file is complete on disk and parseable by the same
 * extractors ExoPlayer uses at playback time.
 */
object DownloadedMediaVerifier {
    private const val MIN_DURATION_FOR_LENGTH_CHECK_SECONDS = 15L
    private const val MIN_DURATION_FRACTION = 0.9

    fun verify(item: DownloadItem, expectedDurationSeconds: Long? = null): MediaVerifyResult {
        val size = runCatching { item.path.fileSize() }.getOrDefault(0L)
        if (size <= 0L) return MediaVerifyResult.Corrupt("empty file")

        if (item.type == FileType.SUBTITLE) return MediaVerifyResult.Ok

        when (IsoBmffBoxScanner.scan(item.path)) {
            IsoBmffScanResult.Truncated -> {
                return MediaVerifyResult.Corrupt("truncated MP4 box")
            }
            IsoBmffScanResult.Ok, IsoBmffScanResult.NotIsoBmff -> Unit
        }

        return try {
            verifySamples(item.path, expectedDurationSeconds)
        } catch (e: Exception) {
            Log.e(TAG(), "media verify failed for ${item.path}: ${e.stackTraceToString()}")
            MediaVerifyResult.Corrupt(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun verifySamples(path: Path, expectedDurationSeconds: Long?): MediaVerifyResult {
        val uri = path.toAndroidUri()
        val fileLength = path.fileSize()
        val dataSource = FileDataSource()
        val output = CollectingExtractorOutput()
        val seekHolder = PositionHolder()
        var extractor: Extractor? = null
        try {
            dataSource.open(DataSpec(uri))
            var input = DefaultExtractorInput(dataSource, 0, fileLength)
            extractor = DefaultExtractorsFactory()
                .createExtractors()
                .firstOrNull { candidate ->
                    try {
                        input.resetPeekPosition()
                        candidate.sniff(input)
                    } catch (_: Exception) {
                        false
                    } finally {
                        input.resetPeekPosition()
                    }
                } ?: return MediaVerifyResult.Corrupt("unrecognized container")

            extractor.init(output)
            var reads = 0
            while (reads++ < MAX_READS) {
                when (val result = extractor.read(input, seekHolder)) {
                    Extractor.RESULT_END_OF_INPUT -> break
                    Extractor.RESULT_SEEK -> {
                        dataSource.close()
                        val position = seekHolder.position
                        dataSource.open(
                            DataSpec.Builder()
                                .setUri(uri)
                                .setPosition(position)
                                .build()
                        )
                        // same as ExoPlayer's own load loop: reposition the input, no extractor.seek()
                        input = DefaultExtractorInput(
                            dataSource,
                            position,
                            fileLength - position
                        )
                    }
                    Extractor.RESULT_CONTINUE -> Unit
                    else -> return MediaVerifyResult.Corrupt("extractor result $result")
                }
            }

            if (output.sampleCount == 0) {
                return MediaVerifyResult.Corrupt("no media samples")
            }

            val expectedUs = expectedDurationSeconds
                ?.takeIf { it >= MIN_DURATION_FOR_LENGTH_CHECK_SECONDS }
                ?.times(1_000_000L)
            if (expectedUs != null && output.maxTimeUs > 0L &&
                output.maxTimeUs < (expectedUs * MIN_DURATION_FRACTION).toLong()
            ) {
                // Missing tail segments cannot be resumed (the provider already reported
                // completion), so treat as corrupt and start over.
                return MediaVerifyResult.Corrupt(
                    "duration ${output.maxTimeUs / 1000}ms < expected ${expectedDurationSeconds}s"
                )
            }
            return MediaVerifyResult.Ok
        } finally {
            extractor?.release()
            try {
                dataSource.close()
            } catch (_: Exception) {
            }
        }
    }

    private const val MAX_READS = 5_000_000

    private class CollectingExtractorOutput : ExtractorOutput {
        private val tracks = mutableMapOf<Int, RecordingTrackOutput>()
        val sampleCount get() = tracks.values.sumOf { it.sampleCount }
        val maxTimeUs get() = tracks.values.maxOfOrNull { it.maxTimeUs } ?: 0L

        override fun track(id: Int, type: Int): TrackOutput =
            tracks.getOrPut(id) { RecordingTrackOutput() }

        override fun endTracks() = Unit
        override fun seekMap(seekMap: SeekMap) = Unit
    }

    private class RecordingTrackOutput : TrackOutput {
        var maxTimeUs: Long = 0
        var sampleCount: Int = 0
        private val scratch = ByteArray(16 * 1024)

        override fun durationUs(durationUs: Long) = Unit
        override fun format(format: Format) = Unit

        override fun sampleData(
            input: DataReader,
            length: Int,
            allowEndOfInput: Boolean,
            sampleDataPart: Int
        ): Int {
            var remaining = length
            var total = 0
            while (remaining > 0) {
                val toRead = minOf(remaining, scratch.size)
                val read = input.read(scratch, 0, toRead)
                if (read == C.RESULT_END_OF_INPUT) {
                    if (allowEndOfInput && total == 0) return C.RESULT_END_OF_INPUT
                    throw EOFException()
                }
                remaining -= read
                total += read
            }
            return total
        }

        override fun sampleData(
            data: ParsableByteArray,
            length: Int,
            sampleDataPart: Int
        ) {
            data.skipBytes(length)
        }

        override fun sampleMetadata(
            timeUs: Long,
            flags: Int,
            size: Int,
            offset: Int,
            cryptoData: TrackOutput.CryptoData?
        ) {
            sampleCount++
            if (timeUs > maxTimeUs) maxTimeUs = timeUs
        }
    }
}
