package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeWatchHistoryFileItem(
    val activityControls: List<String>,
    val header: String,
    val products: List<String>,
    val subtitles: List<YouTubeWatchHistoryChannelInfo>,
    val time: String,
    val title: String,
    val titleUrl: String
)