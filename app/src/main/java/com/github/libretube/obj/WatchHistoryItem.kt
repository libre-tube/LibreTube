package com.github.libretube.obj

data class WatchHistoryItem(
    val videoId: String? = null,
    val title: String? = null,
    val uploadDate: String? = null,
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null
)
