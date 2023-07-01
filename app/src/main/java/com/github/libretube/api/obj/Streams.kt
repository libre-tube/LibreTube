package com.github.libretube.api.obj

import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.parcelable.DownloadData
import java.nio.file.Paths
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Streams(
    val title: String,
    val description: String,
    val uploadDate: LocalDate,
    val uploader: String,
    val uploaderUrl: String,
    val uploaderAvatar: String? = null,
    val thumbnailUrl: String,
    val category: String,
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
    fun toDownloadItems(downloadData: DownloadData): List<DownloadItem> {
        val (id, name, videoFormat, videoQuality, audioFormat, audioQuality, subCode) = downloadData
        val items = mutableListOf<DownloadItem>()

        if (!videoQuality.isNullOrEmpty() && !videoFormat.isNullOrEmpty()) {
            val stream = videoStreams.find {
                it.quality == videoQuality && it.format == videoFormat
            }
            stream?.toDownloadItem(FileType.VIDEO, id, name)?.let { items.add(it) }
        }

        if (!audioQuality.isNullOrEmpty() && !audioFormat.isNullOrEmpty()) {
            val stream = audioStreams.find {
                it.quality == audioQuality && it.format == audioFormat
            }
            stream?.toDownloadItem(FileType.AUDIO, id, name)?.let { items.add(it) }
        }

        if (!subCode.isNullOrEmpty()) {
            items.add(
                DownloadItem(
                    type = FileType.SUBTITLE,
                    videoId = id,
                    fileName = "${name}_$subCode.srt",
                    path = Paths.get(""),
                    url = subtitles.find {
                        it.code == subCode
                    }?.url?.let { ProxyHelper.unwrapUrl(it) }
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
