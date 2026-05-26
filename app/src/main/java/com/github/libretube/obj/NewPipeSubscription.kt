package com.github.libretube.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewPipeSubscription(
    val name: String,
    @SerialName("service_id") val serviceId: Int,
    val url: String,
)
