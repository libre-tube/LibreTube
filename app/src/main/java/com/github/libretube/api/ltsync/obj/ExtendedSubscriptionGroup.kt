package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtendedSubscriptionGroup (
    @SerialName(value = "channels")
    val channels: List<Channel>,
    @SerialName(value = "group")
    val group: SubscriptionGroup
)
