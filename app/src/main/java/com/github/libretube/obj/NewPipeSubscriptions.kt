package com.github.libretube.obj

data class NewPipeSubscriptions(
    val app_version: String = "",
    val app_version_int: Int = 0,
    val subscriptions: List<NewPipeSubscription>? = null
)
