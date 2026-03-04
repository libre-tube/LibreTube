package com.github.libretube.api.obj

import android.os.Parcelable
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toLocalDate
import com.github.libretube.json.SafeInstantSerializer
import com.github.libretube.parcelable.DownloadData
import kotlinx.datetime.Instant
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Streams(
    var title: String,
    val description: String,

    @Serializable(SafeInstantSerializer::class)
    @SerialName("uploadDate")
    @IgnoredOnParcel
    val uploadTimestamp: Instant? = null,
    val uploaded: Long? = null,

    val uploader: String,
    val uploaderUrl: String?,
    val uploaderAvatar: String? = null,
    var thumbnailUrl: String,
    val category: String,
    val license: String = "YouTube licence",
    val visibility: String = "public",
    val tags: List<String> = emptyList(),
    val metaInfo: List<MetaInfo> = emptyList(),
    val hls: String? = null,
    val dash: String? = null,
    val uploaderVerified: Boolean,
    val duration: Long,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val audioStreams: List<PipedStream> = emptyList(),
    val videoStreams: List<PipedStream> = emptyList(),
    var relatedStreams: List<StreamItem> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val livestream: Boolean = false,
    val proxyUrl: String? = null,
    val chapters: List<ChapterSegment> = emptyList(),
    val uploaderSubscriberCount: Long = 0,
    val previewFrames: List<PreviewFrames> = emptyList()
): Parcelable {
    @IgnoredOnParcel
    val isLive = livestream || duration <= 0

    fun toDownloadItems(downloadData: DownloadData): List<DownloadItem> {
        val (id, videoFormat, videoQuality, audioFormat, audioQuality, audioTrackLocale, subCode) = downloadData
        val items = mutableListOf<DownloadItem>()

        if (!videoQuality.isNullOrEmpty() && !videoFormat.isNullOrEmpty()) {
            val stream = videoStreams.find {
                it.quality == videoQuality && it.format == videoFormat
            }
            stream?.toDownloadItem(FileType.VIDEO, id)?.let { items.add(it) }
        }

        if (!audioQuality.isNullOrEmpty() && !audioFormat.isNullOrEmpty()) {
            val stream = audioStreams.find {
                it.quality == audioQuality && it.format == audioFormat && it.audioTrackLocale == audioTrackLocale
            }
            stream?.toDownloadItem(FileType.AUDIO, id)?.let { items.add(it) }
        }

        if (!subCode.isNullOrEmpty()) {
            val subtitle = subtitles.find { it.code == subCode }
            subtitle?.toDownloadItem(id)?.let { items.add(it) }
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
            uploadedDate = uploadTimestamp?.toLocalDate()?.toString(),
            uploaded = uploaded ?: uploadTimestamp?.toEpochMilliseconds() ?: 0,
            duration = duration,
            views = views,
            uploaderVerified = uploaderVerified,
            shortDescription = description
        )
    }

    companion object {
        const val CATEGORY_MUSIC = "Music"
    }
}
