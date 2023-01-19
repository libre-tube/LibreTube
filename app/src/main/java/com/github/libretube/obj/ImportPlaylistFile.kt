package com.github.libretube.obj

data class ImportPlaylistFile(
    val format: String? = null,
    val version: Int? = null,
    val playlists: List<ImportPlaylist> = emptyList()
)
