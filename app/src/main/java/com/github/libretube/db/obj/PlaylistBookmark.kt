package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "playlistBookmark")
data class PlaylistBookmark(
    @PrimaryKey
    val playlistId: String = "",
    var playlistName: String? = null,
    var thumbnailUrl: String? = null,
    var uploader: String? = null,
    var uploaderUrl: String? = null,
    var uploaderAvatar: String? = null
)
