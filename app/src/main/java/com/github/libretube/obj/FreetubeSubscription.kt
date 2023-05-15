package com.github.libretube.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FreetubeSubscription(
    @SerialName("name") val name: String,
    @SerialName("id") val serviceId: String,
    val url: String = "https://www.youtube.com/channel/$serviceId",
)
