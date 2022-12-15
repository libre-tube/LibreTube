package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Streams(
    val title: String? = null,
    val description: String? = null,
    val uploadDate: String? = null,
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val thumbnailUrl: String? = null,
    val hls: String? = null,
    val dash: String? = null,
    val lbryId: String? = null,
    val uploaderVerified: Boolean? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val likes: Long? = null,
    val dislikes: Long? = null,
    val audioStreams: List<PipedStream>? = null,
    val videoStreams: List<PipedStream>? = null,
    val relatedStreams: List<StreamItem>? = null,
    val subtitles: List<Subtitle>? = null,
    val livestream: Boolean? = null,
    val proxyUrl: String? = null,
    val chapters: List<ChapterSegment>? = null,
    val uploaderSubscriberCount: Long? = null,
    val previewFrames: List<PreviewFrames>? = null
)
