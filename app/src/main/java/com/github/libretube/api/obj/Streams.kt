package com.github.libretube.api.obj

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
)
