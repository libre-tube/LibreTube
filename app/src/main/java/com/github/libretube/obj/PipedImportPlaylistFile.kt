package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class PipedImportPlaylistFile(
    val format: String,
    val version: Int,
    val playlists: List<PipedImportPlaylist> = emptyList(),
)
