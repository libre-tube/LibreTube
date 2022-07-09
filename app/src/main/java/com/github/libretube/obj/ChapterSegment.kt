package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChapterSegment(
    var title: String?,
    var image: String?,
    var start: Long?
) {
    constructor() : this("", "", -1)
}
