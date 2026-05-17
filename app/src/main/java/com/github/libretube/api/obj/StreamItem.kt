package com.github.libretube.api.obj

import android.os.Parcelable
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toLocalDate
import com.github.libretube.helpers.ProxyHelper
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
    val isLive get() = !isShort && ((duration == null) || (duration <= 0L))
    val isUpcoming get() = uploaded > System.currentTimeMillis()

    fun toLocalPlaylistItem(playlistId: String): LocalPlaylistItem {
        return LocalPlaylistItem(
            playlistId = playlistId.toInt(),
            videoId = url!!.toID(),
            title = title,
            thumbnailUrl = thumbnail?.let { ProxyHelper.unwrapUrl(it) },
            uploader = uploaderName,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar?.let { ProxyHelper.unwrapUrl(it) },
            uploadDate = uploadedDate,
            duration = duration
        )
    }

    fun toFeedItem(existingFeedItem: SubscriptionsFeedItem? = null) = SubscriptionsFeedItem(
        videoId = url!!.toID(),
        title = title ?: existingFeedItem?.title,
        thumbnail = thumbnail ?: existingFeedItem?.thumbnail,
        uploaderName = uploaderName ?: existingFeedItem?.uploaderName,
        uploaded = uploaded.takeIf { it > 0 } ?: existingFeedItem?.uploaded ?: uploaded,
        uploaderAvatar = uploaderAvatar ?: existingFeedItem?.uploaderAvatar,
        uploaderUrl = uploaderUrl ?: existingFeedItem?.uploaderUrl,
        duration = duration ?: existingFeedItem?.duration,
        uploaderVerified = uploaderVerified ?: existingFeedItem?.uploaderVerified ?: false,
        shortDescription = shortDescription ?: existingFeedItem?.shortDescription,
        views = views ?: existingFeedItem?.views,
        isShort = isShort || existingFeedItem?.isShort == true
    )
    
    fun toWatchHistoryItem(videoId: String) = WatchHistoryItem(
        videoId = videoId,
        title = title,
        uploadDate = uploaded.toLocalDate(),
        uploader = uploaderName,
        uploaderUrl = uploaderUrl?.toID(),
        uploaderAvatar = uploaderAvatar?.let { ProxyHelper.unwrapUrl(it) },
        thumbnailUrl = thumbnail?.let { ProxyHelper.unwrapUrl(it) },
        duration = duration
    )

    companion object {
        const val TYPE_STREAM = "stream"
        const val TYPE_CHANNEL = "channel"
        const val TYPE_PLAYLIST = "playlist"
    }
}
