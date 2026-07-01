package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LoginResponse (
    @SerialName(value = "jwt")
    val jwt: String
) {
}

