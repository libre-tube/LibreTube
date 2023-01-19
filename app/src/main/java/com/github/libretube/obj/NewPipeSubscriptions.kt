package com.github.libretube.obj

import kotlinx.serialization.Serializable

@Serializable
data class NewPipeSubscriptions(
    val app_version: String = "",
    val app_version_int: Int = 0,
    val subscriptions: List<NewPipeSubscription> = emptyList()
)
