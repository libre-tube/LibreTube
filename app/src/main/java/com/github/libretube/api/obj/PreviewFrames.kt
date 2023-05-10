package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class PreviewFrames(
    val urls: List<String>? = null,
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    val totalCount: Int? = null,
    val durationPerFrame: Int? = null,
    val framesPerPageX: Int? = null,
    val framesPerPageY: Int? = null,
)
