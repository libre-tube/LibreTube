package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChapterSegment(
    var title: String? = null,
    var image: String? = null,
    var start: Long?  = null
)
