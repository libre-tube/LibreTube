package com.github.libretube.api.obj

data class ChannelTabResponse(
    val content: List<ContentItem> = listOf(),
    val nextpage: String? = null
)
