package com.github.libretube.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibreTubeSubscriptions (
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("app_version_int") val appVersionInt: Int = 0,
    val subscriptions: List<LibreTubeSubscription> = emptyList(),
)