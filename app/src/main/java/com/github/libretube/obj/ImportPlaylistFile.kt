package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class ImportPlaylistFile(
    val format: String,
    val version: Int,
    val playlists: List<ImportPlaylist> = emptyList(),
)
