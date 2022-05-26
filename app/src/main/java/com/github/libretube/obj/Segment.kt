package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Segment(
    val actionType: String?,
    val category: String?,
    val segment: List<Float>?
) {
    constructor() : this("", "", arrayListOf())
}
