package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class FreeTubeVideo(
    val videoId: String,
    val title: String,
    val author: String,
    val authorId: String,
)
