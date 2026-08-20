package com.github.libretube.api.ltsync.obj

import com.github.libretube.api.obj.StreamItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CreateVideo(
    @SerialName(value = "duration")
    val duration: Int,

    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "thumbnail_url")
    val thumbnailUrl: String,

    @SerialName(value = "title")
    val title: String,

    /* Upload date as UNIX timestamp (millis). */
    @SerialName(value = "upload_date")
    val uploadDate: Long,

    @SerialName(value = "uploader")
    val uploader: Channel
) {
    fun toStreamItem(): StreamItem = StreamItem(
        url = id,
        title = title,
        type = StreamItem.TYPE_STREAM,
        uploaded = uploadDate,
        thumbnail = thumbnailUrl,
        duration = duration.toLong(),
        uploaderUrl = uploader.id,
        uploaderName = uploader.name,
        uploaderVerified = uploader.verified,
        uploaderAvatar = uploader.avatar,
    )

}
