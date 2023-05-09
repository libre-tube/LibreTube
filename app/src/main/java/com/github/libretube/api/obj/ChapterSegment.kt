package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ChapterSegment(
    val title: String,
    val image: String,
    val start: Long,
)
