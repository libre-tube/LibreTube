package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Subscribed(val subscribed: Boolean? = null)
