package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class RegisterUser (
    @SerialName(value = "name")
    val name: String,

    @SerialName(value = "password")
    val password: String
) {
}

