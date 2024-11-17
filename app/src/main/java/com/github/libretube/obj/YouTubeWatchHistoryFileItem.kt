package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeWatchHistoryFileItem(
    val header: String = "",
    val time: String = "",
    val title: String = "",
    val titleUrl: String = "",
    val activityControls: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val subtitles: List<YouTubeWatchHistoryChannelInfo> = emptyList()
)