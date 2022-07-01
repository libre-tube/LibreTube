package com.github.libretube.obj

data class WatchHistoryItem(
    val videoId: String?,
    val title: String?,
    val uploadDate: String?,
    val uploader: String?,
    val uploaderUrl: String?,
    val uploaderAvatar: String?,
    val thumbnailUrl: String?,
    val duration: Int?
)
