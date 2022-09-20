package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Streams(
    val title: String?,
    val description: String?,
    val uploadDate: String?,
    val uploader: String?,
    val uploaderUrl: String?,
    val uploaderAvatar: String?,
    val thumbnailUrl: String?,
    val hls: String?,
    val dash: String?,
    val lbryId: String?,
    val uploaderVerified: Boolean?,
    val duration: Long?,
    val views: Long?,
    val likes: Long?,
    val dislikes: Long?,
    val audioStreams: List<com.github.libretube.api.obj.PipedStream>?,
    val videoStreams: List<com.github.libretube.api.obj.PipedStream>?,
    val relatedStreams: List<com.github.libretube.api.obj.StreamItem>?,
    val subtitles: List<com.github.libretube.api.obj.Subtitle>?,
    val livestream: Boolean?,
    val proxyUrl: String?,
    val chapters: List<com.github.libretube.api.obj.ChapterSegment>?
) {
    constructor() : this(
        "", "", "", "", "", "", "", "", "", "", null, -1, -1, -1, -1, emptyList(), emptyList(),
        emptyList(), emptyList(), null, "", emptyList()
    )
}
