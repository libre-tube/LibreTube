package com.github.libretube.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FreeTubeImportPlaylist(
    @SerialName("playlistName") val name: String = "",
    // if type is `video` -> https://www.youtube.com/watch?v=IT734HriiHQ, works with shorts too
    var videos: List<FreeTubeVideo> = listOf()
)
