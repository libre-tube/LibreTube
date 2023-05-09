package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val url: String,
    val name: String,
    val avatar: String,
    val verified: Boolean,
)
