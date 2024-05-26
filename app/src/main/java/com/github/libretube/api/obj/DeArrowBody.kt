package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class DeArrowBody(
    val videoID: String,
    val userID: String,
    val userAgent: String,
    val title: DeArrowSubmitTitle?,
    val thumbnail: DeArrowSubmitThumbnail?,
    val downvote: Boolean = false
)

@Serializable
data class DeArrowSubmitTitle(
    val title: String
)

@Serializable
data class DeArrowSubmitThumbnail(
    val timestamp: Float
)