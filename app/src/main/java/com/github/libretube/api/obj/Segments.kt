package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Segments(
    val segments: MutableList<com.github.libretube.api.obj.Segment> = arrayListOf()
)
