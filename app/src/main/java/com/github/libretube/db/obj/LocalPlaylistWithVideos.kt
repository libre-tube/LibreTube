package com.github.libretube.db.obj

import androidx.room.Embedded
import androidx.room.Relation

data class LocalPlaylistWithVideos(
    @Embedded val playlist: LocalPlaylist = LocalPlaylist(),
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val videos: List<LocalPlaylistItem> = listOf()
)
