package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CreatePlaylist(

    @SerialName(value = "description")
    val description: String,

    @SerialName(value = "title")
    val title: String,

    @SerialName(value = "thumbnail_url")
    val thumbnailUrl: String? = null

)

