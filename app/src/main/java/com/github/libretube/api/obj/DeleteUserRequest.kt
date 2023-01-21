package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class DeleteUserRequest(val password: String)
