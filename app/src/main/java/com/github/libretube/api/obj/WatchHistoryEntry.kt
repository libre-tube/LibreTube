package com.github.libretube.api.obj

data class WatchHistoryEntryMetadata(
    val videoId: String,
    val addedDate: Long,
    val finished: Boolean,
    val positionMillis: Long? = null,
)

data class WatchHistoryEntry(
    val metadata: WatchHistoryEntryMetadata,
    val video: StreamItem
)