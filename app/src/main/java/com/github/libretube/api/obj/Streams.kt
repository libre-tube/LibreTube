package com.github.libretube.api.obj

import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Streams(
    val title: String,
    val description: String,
    val uploadDate: LocalDate,
    val uploader: String,
    val uploaderUrl: String,
    val uploaderAvatar: String,
    val thumbnailUrl: String,
    val hls: String? = null,
    val dash: String? = null,
    val lbryId: String? = null,
    val uploaderVerified: Boolean,
    val duration: Long,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val audioStreams: List<PipedStream> = emptyList(),
    val videoStreams: List<PipedStream> = emptyList(),
    val relatedStreams: List<StreamItem> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val livestream: Boolean = false,
    val proxyUrl: String? = null,
    val chapters: List<ChapterSegment> = emptyList(),
    val uploaderSubscriberCount: Long = 0,
    val previewFrames: List<PreviewFrames> = emptyList()
) {
    fun toDownloadItems(
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
                    fileName = stream?.getQualityString(fileName).orEmpty(),
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
                    fileName = stream?.getQualityString(fileName).orEmpty(),
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

    fun toStreamItem(videoId: String): StreamItem {
        return StreamItem(
            url = videoId,
            title = title,
            thumbnail = thumbnailUrl,
            uploaderName = uploader,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar,
            uploadedDate = uploadDate.toString(),
            uploaded = null,
            duration = duration,
            views = views,
            uploaderVerified = uploaderVerified,
            shortDescription = description
        )
    }
}
