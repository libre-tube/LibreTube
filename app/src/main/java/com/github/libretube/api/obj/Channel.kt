package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val description: String? = null,
    val nextpage: String? = null,
    val subscriberCount: Long = 0,
    val verified: Boolean = false,
    val relatedStreams: List<StreamItem> = emptyList(),
    val tabs: List<ChannelTab> = emptyList(),
)
