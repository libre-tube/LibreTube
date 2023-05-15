package com.github.libretube.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FreetubeSubscriptions (
    @SerialName("_id") val id: String = "",
    @SerialName("name") val name: String = "",
    val subscriptions: List<FreetubeSubscription> = emptyList(),
)