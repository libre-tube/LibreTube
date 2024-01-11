package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class PipedBackupFile(
    val format: String,
    val version: Int,
    val playlists: List<PipedImportPlaylist> = emptyList(),
    val groups: List<PipedChannelGroup> = emptyList()
)
