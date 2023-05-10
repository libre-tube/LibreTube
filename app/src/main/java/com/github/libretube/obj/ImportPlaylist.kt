package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class ImportPlaylist(
    var name: String? = null,
    val type: String? = null,
    val visibility: String? = null,
    var videos: List<String> = listOf(),
)
