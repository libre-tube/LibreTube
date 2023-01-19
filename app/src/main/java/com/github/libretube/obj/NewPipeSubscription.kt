package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class NewPipeSubscription(
    val name: String? = null,
    val service_id: Int? = null,
    val url: String? = null
)
