package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocalPlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo var playlistId: Int = 0,
    @ColumnInfo val videoId: String = "",
    @ColumnInfo val title: String? = null,
    @ColumnInfo val uploadDate: String? = null,
    @ColumnInfo val uploader: String? = null,
    @ColumnInfo val uploaderUrl: String? = null,
    @ColumnInfo val uploaderAvatar: String? = null,
    @ColumnInfo val thumbnailUrl: String? = null,
    @ColumnInfo val duration: Long? = null
)
