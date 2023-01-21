package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "watchHistoryItem")
data class WatchHistoryItem(
    @PrimaryKey val videoId: String = "",
    @ColumnInfo val title: String? = null,
    @ColumnInfo val uploadDate: String? = null,
    @ColumnInfo val uploader: String? = null,
    @ColumnInfo val uploaderUrl: String? = null,
    @ColumnInfo var uploaderAvatar: String? = null,
    @ColumnInfo var thumbnailUrl: String? = null,
    @ColumnInfo val duration: Long? = null
)
