package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SubscriptionGroup(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "title")
    val title: String
)

