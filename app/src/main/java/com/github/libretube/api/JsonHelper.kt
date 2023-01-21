package com.github.libretube.api

import kotlinx.serialization.json.Json

object JsonHelper {
    val json = Json {
        ignoreUnknownKeys = true
    }
}
