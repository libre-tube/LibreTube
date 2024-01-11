package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class PipedChannelGroup(
    val groupName: String,
    val channels: List<String>
)
