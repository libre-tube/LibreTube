package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class SegmentData(
    val hash: String? = null,
    val segments: List<Segment> = listOf(),
    val videoID: String? = null
)
