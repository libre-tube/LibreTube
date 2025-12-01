package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeWatchHistoryChannelInfo(
    val name: String? = null,
    val url: String? = null
)