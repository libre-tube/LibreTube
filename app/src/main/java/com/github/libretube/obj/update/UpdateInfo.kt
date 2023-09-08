package com.github.libretube.obj.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    @SerialName("html_url") val htmlUrl: String,
    val name: String
)
