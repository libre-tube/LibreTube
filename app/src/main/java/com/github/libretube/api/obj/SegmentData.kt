package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SegmentData(
    val hash: String? = null,
    val segments: List<Segment> = listOf(),
    val videoID: String? = null
)
