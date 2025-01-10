package com.github.libretube.api.obj

import android.os.Parcelable
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.extensions.toID
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class StreamItem(
    val url: String? = null,
    val type: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean? = null,
    val uploaded: Long = 0,
    val shortDescription: String? = null,
    val isShort: Boolean = false
) : Parcelable {
    val isLive get() = (duration == null) || (duration <= 0L)

    fun toLocalPlaylistItem(playlistId: String): LocalPlaylistItem {
        return LocalPlaylistItem(
            playlistId = playlistId.toInt(),
            videoId = url!!.toID(),
            title = title,
            thumbnailUrl = thumbnail,
            uploader = uploaderName,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar,
            uploadDate = uploadedDate,
            duration = duration
        )
    }

    fun toFeedItem() = SubscriptionsFeedItem(
        videoId = url!!.toID(),
        title = title,
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaded = uploaded,
        uploaderAvatar = uploaderAvatar,
        uploaderUrl = uploaderUrl,
        duration = duration,
        uploaderVerified = uploaderVerified ?: false,
        shortDescription = shortDescription,
        views = views,
        isShort = isShort
    )

    companion object {
        const val TYPE_STREAM = "stream"
        const val TYPE_CHANNEL = "channel"
        const val TYPE_PLAYLIST = "playlist"
    }
}
