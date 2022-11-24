package com.github.libretube.obj

data class ShareData(
    val currentChannel: String? = null,
    val currentVideo: String? = null,
    val currentPlaylist: String? = null,
    var currentPosition: Long? = null
)
