package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ChannelTab(
    val name: String,
    val data: String
)
