package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class EditPlaylistBody(
    val playlistId: String? = null,
    val videoId: String? = null,
    val videoIds: List<String> = emptyList(),
    val newName: String? = null,
    val description: String? = null,
    val index: Int = -1,
)
