package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Playlists(
    val id: String? = null,
    var name: String? = null,
    val shortDescription: String? = null,
    val thumbnail: String? = null,
    val videos: Long = 0,
)
