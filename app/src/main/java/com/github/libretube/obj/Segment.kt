package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Segment(
    val actionType: String? = null,
    val category: String? = null,
    val segment: List<Float>? = arrayListOf()
)