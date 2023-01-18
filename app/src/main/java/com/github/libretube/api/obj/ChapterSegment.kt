package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ChapterSegment(
    var title: String? = null,
    var image: String? = null,
    var start: Long? = null
)
