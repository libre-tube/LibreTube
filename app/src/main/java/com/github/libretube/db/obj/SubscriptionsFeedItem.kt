package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toLocalDate

@Entity(tableName = "feedItem")
data class SubscriptionsFeedItem(
    @PrimaryKey
    val videoId: String,
    val title: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean,
    val uploaded: Long = 0,
    val shortDescription: String? = null,
    val isShort: Boolean = false
) {
    fun toStreamItem() = StreamItem(
        url = videoId,
        type = StreamItem.TYPE_STREAM,
        title = title,
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaded = uploaded,
        uploadedDate = uploaded.toLocalDate().toString(),
        uploaderAvatar = uploaderAvatar,
        uploaderUrl = uploaderUrl,
        duration = duration,
        uploaderVerified = uploaderVerified,
        shortDescription = shortDescription,
        views = views,
        isShort = isShort
    )
}