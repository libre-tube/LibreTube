package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WatchedState(val value: String) {

    @SerialName(value = "Planned")
    Planned("Planned"),

    @SerialName(value = "Watching")
    Watching("Watching"),

    @SerialName(value = "Completed")
    Completed("Completed"),

    @SerialName(value = "Dropped")
    Dropped("Dropped")
}
