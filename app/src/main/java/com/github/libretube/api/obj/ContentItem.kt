package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val url: String,
    val type: String,
    var thumbnail: String,
    // Video only attributes
    var title: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val duration: Long = -1,
    val views: Long = -1,
    val isShort: Boolean? = null,
    val uploaderVerified: Boolean? = null,
    val uploaderName: String? = null,
    val uploaded: Long? = null,
    val shortDescription: String? = null,
    // Channel and Playlist attributes
    val name: String? = null,
    val description: String? = null,
    val subscribers: Long = -1,
    val videos: Long = -1,
    val verified: Boolean? = null
) {
    fun toStreamItem() = StreamItem(
        url = url,
        type = StreamItem.TYPE_STREAM,
        title = title,
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaded = uploaded,
        uploaderAvatar = uploaderAvatar,
        uploaderUrl = uploaderUrl,
        duration = duration,
        uploaderVerified = uploaderVerified,
        shortDescription = shortDescription,
        views = views,
        isShort = isShort == true
    )
}
