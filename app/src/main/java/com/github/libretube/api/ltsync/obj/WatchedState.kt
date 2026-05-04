package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WatchedState {

    @SerialName(value = "planned")
    Planned,

    @SerialName(value = "watching")
    Watching,

    @SerialName(value = "completed")
    Completed,

    @SerialName(value = "dropped")
    Dropped
}
