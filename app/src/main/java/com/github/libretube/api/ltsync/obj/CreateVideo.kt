package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CreateVideo (
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
}
