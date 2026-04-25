package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CreatePlaylist (

    @SerialName(value = "description")
    val description: kotlin.String,

    @SerialName(value = "title")
    val title: kotlin.String,

    @SerialName(value = "thumbnail_url")
    val thumbnailUrl: kotlin.String? = null

) {


}

