package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class DeArrowTitle(
    val UUID: String,
    val locked: Boolean,
    val original: Boolean,
    val title: String,
    val votes: Int
)
