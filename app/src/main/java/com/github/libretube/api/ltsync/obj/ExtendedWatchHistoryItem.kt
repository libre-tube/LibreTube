package com.github.libretube.api.ltsync.obj


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtendedWatchHistoryItem (

    @SerialName(value = "metadata")
    val metadata: WatchHistoryItem,

    @SerialName(value = "video")
    val video: CreateVideo

) {
}
