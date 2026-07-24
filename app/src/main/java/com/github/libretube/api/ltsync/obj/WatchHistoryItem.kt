package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchHistoryItem (
    /* Date as UNIX timestamp (millis). */
    @SerialName(value = "added_date")
    val addedDate: kotlin.Long,

    @Contextual @SerialName(value = "watched_state")
    val watchedState: WatchedState,

    @SerialName(value = "position_millis")
    val positionMillis: kotlin.Int? = null
)
