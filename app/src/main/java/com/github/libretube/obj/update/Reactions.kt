package com.github.libretube.obj.update

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reactions(
    val confused: Int? = null,
    val eyes: Int? = null,
    val heart: Int? = null,
    val hooray: Int? = null,
    val laugh: Int? = null,
    val rocket: Int? = null,
    val total_count: Int? = null,
    val url: String? = null
)
