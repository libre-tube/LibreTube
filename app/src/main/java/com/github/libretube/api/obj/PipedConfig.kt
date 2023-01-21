package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class PipedConfig(
    val donationUrl: String? = null,
    val statusPageUrl: String? = null,
    val imageProxyUrl: String? = null
)
