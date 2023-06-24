package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ChannelTabResponse(
    val content: List<ContentItem> = emptyList(),
    val nextpage: String? = null
)
