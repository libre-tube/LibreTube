package com.github.libretube.extensions

import com.github.libretube.api.obj.Streams
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType

fun Streams.toDownloadItems(
    videoId: String,
    fileName: String,
    videoFormat: String?,
    videoQuality: String?,
    audioFormat: String?,
    audioQuality: String?,
    subtitleCode: String?
): List<DownloadItem> {
    val items = mutableListOf<DownloadItem>()

    if (!videoQuality.isNullOrEmpty() && !videoFormat.isNullOrEmpty()) {
        val stream = videoStreams.find { it.quality == videoQuality && it.format == videoFormat }
        items.add(
            DownloadItem(
                type = FileType.VIDEO,
                videoId = videoId,
                fileName = stream.qualityString(fileName),
                path = "",
                url = stream?.url,
                format = videoFormat,
                quality = videoQuality
            )
        )
    }

    if (!audioQuality.isNullOrEmpty() && !audioFormat.isNullOrEmpty()) {
        val stream = audioStreams.find { it.quality == audioQuality && it.format == audioFormat }
        items.add(
            DownloadItem(
                type = FileType.AUDIO,
                videoId = videoId,
                fileName = stream.qualityString(fileName),
                path = "",
                url = stream?.url,
                format = audioFormat,
                quality = audioQuality
            )
        )
    }

    if (!subtitleCode.isNullOrEmpty()) {
        items.add(
            DownloadItem(
                type = FileType.SUBTITLE,
                videoId = videoId,
                fileName = "${fileName}_$subtitleCode.srt",
                path = "",
                url = subtitles.find { it.code == subtitleCode }?.url
            )
        )
    }

    return items
}
