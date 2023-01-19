package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ChapterSegment(
    val title: String? = null,
    val image: String? = null,
    val start: Long? = null
)
