package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreviewFrames(
    val urls: List<String>? = null,
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    val totalCount: Int? = null,
    val durationPerFrame: Int? = null,
    val framesPerPageX: Int? = null,
    val framesPerPageY: Int? = null
)
