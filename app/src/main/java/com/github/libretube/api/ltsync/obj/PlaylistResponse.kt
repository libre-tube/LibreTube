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


}

