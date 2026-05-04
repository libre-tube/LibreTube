package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Channel(
    @SerialName(value = "avatar")
    val avatar: String,

    @SerialName(value = "id")
    val id: String,

    @SerialName(value = "name")
    val name: String,

    @SerialName(value = "verified")
    val verified: Boolean

) {
}
