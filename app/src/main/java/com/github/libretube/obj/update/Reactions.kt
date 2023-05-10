package com.github.libretube.obj.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reactions(
    val confused: Int,
    val eyes: Int,
    val heart: Int,
    val hooray: Int,
    val laugh: Int,
    val rocket: Int,
    @SerialName("total_count") val totalCount: Int,
    val url: String,
)
