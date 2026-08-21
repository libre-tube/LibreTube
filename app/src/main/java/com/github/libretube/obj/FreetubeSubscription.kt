package com.github.libretube.obj

import com.github.libretube.constants.YouTubeConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FreetubeSubscription(
    val name: String,
    @SerialName("id") val channelId: String,
    val url: String = "${YouTubeConstants.FRONTEND_URL}${YouTubeConstants.CHANNEL_PATH}$channelId"
)
