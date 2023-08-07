package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class MetaInfo(
    val title: String,
    val description: String,
    val urls: List<String>,
    val urlTexts: List<String>
)
