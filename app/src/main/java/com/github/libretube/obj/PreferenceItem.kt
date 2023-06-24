package com.github.libretube.obj

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class PreferenceItem(
    val key: String? = null,
    val value: JsonPrimitive = JsonNull
)
