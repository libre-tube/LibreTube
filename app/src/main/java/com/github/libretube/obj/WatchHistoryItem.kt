package com.github.libretube.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchHistoryItem")
data class WatchHistoryItem(
    @PrimaryKey val videoId: String? = null,
    @ColumnInfo val title: String? = null,
    @ColumnInfo val uploadDate: String? = null,
    @ColumnInfo val uploader: String? = null,
    @ColumnInfo val uploaderUrl: String? = null,
    @ColumnInfo val uploaderAvatar: String? = null,
    @ColumnInfo val thumbnailUrl: String? = null,
    @ColumnInfo val duration: Long? = null
)
