package com.github.libretube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DeleteUser (
    @SerialName(value = "password")
    val password: String
) {
}

