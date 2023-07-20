package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class DeArrowThumbnail(
    val UUID: String,
    val locked: Boolean,
    val original: Boolean,
    val thumbnail: String? = null,
    val timestamp: Float?,
    val votes: Int
)
