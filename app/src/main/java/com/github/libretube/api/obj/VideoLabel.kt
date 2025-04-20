package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class VideoLabelData(
    val videoID: String,
    val segments: List<VideoLabel>,
)

@Serializable
data class VideoLabel(
    val category: String
)
