package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.libretube.api.obj.StreamItem
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class LocalPlaylistItem(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo var playlistId: Int = 0,
    @ColumnInfo val videoId: String = "",
    @ColumnInfo val title: String? = null,
    @ColumnInfo val uploadDate: String? = null,
    @ColumnInfo val uploader: String? = null,
    @ColumnInfo val uploaderUrl: String? = null,
    @ColumnInfo val uploaderAvatar: String? = null,
    @ColumnInfo val thumbnailUrl: String? = null,
    @ColumnInfo val duration: Long? = null
) {
    fun toStreamItem(): StreamItem {
        return StreamItem(
            url = videoId,
            title = title,
            thumbnail = thumbnailUrl,
            uploaderName = uploader,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar,
            uploadedDate = uploadDate,
            duration = duration
        )
    }
}
