package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlaylistId(
    var playlistId: String? = null,
    var videoId: String? = null,
    var index: Int = -1,
)
