package com.github.libretube.helpers

import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import com.github.libretube.extensions.getWhileDigit
import com.github.libretube.parcelable.DownloadData

internal object PlaylistDownloadSelection {
    fun createDownloadData(
        videoId: String,
        streams: Streams,
        maxVideoQuality: Int?,
        maxAudioQuality: Int?,
        audioLanguage: String?,
        captionLanguage: String?
    ): DownloadData {
        val videoStream = getStream(streams.videoStreams, maxVideoQuality)
        val matchingAudioStreams = streams.audioStreams.filter {
            it.audioTrackLocale == audioLanguage
        }
        val audioStream = getStream(
            matchingAudioStreams.ifEmpty { streams.audioStreams }, maxAudioQuality
        )

        return DownloadData(
            videoId = videoId,
            videoFormat = videoStream?.format,
            videoQuality = videoStream?.quality,
            audioFormat = audioStream?.format,
            audioQuality = audioStream?.quality,
            audioLanguage = audioStream?.audioTrackLocale,
            subtitleCode = captionLanguage.takeIf {
                streams.subtitles.any { it.code == captionLanguage }
            }
        )
    }

    private fun getStream(streams: List<PipedStream>, maxQuality: Int?): PipedStream? {
        val maxStreamQuality = maxQuality ?: return null
        val sortedStreams = streams.sortedBy { it.quality.getWhileDigit() }

        // Prefer the highest quality below the limit, or the lowest available quality.
        return sortedStreams
            .lastOrNull { it.quality.getWhileDigit()!! <= maxStreamQuality }
            ?: sortedStreams.firstOrNull()
    }
}
