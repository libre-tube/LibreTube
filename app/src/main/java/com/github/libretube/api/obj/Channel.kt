package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bannerUrl: String? = null,
    val description: String,
    val nextpage: String? = null,
    val subscriberCount: Long = 0,
    val verified: Boolean = false,
    val relatedStreams: List<StreamItem> = emptyList(),
    val tabs: List<ChannelTab> = emptyList()
)
