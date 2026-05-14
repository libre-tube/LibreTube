package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PlaylistResponse (
    @SerialName(value = "playlist")
    val playlist: Playlist,

    @SerialName(value = "videos")
    val videos: List<CreateVideo>
) {
    fun toPipedPlaylist(): com.github.libretube.api.obj.Playlist = com.github.libretube.api.obj.Playlist(
        name = playlist.title,
        description = playlist.description,
        thumbnailUrl = playlist.thumbnailUrl,
        videos = playlist.videoCount.toInt(),
        relatedStreams = videos.map { it.toStreamItem() }
    )
}

