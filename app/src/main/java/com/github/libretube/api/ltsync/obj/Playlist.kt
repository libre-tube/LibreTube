package com.github.libretube.api.ltsync.obj

import com.github.libretube.api.obj.Playlists
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Playlist(
    @SerialName(value = "description")
    val description: String,

    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "title")
    val title: String,

    @SerialName(value = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerialName(value = "video_count")
    val videoCount: Long = 0,
) {
    fun toPipedPlaylists(): Playlists = Playlists(
        id = id,
        name = title,
        shortDescription = description,
        thumbnail = thumbnailUrl,
        videos = videoCount
    )

}

