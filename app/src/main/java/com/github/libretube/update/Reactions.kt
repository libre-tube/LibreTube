package com.github.libretube.update

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reactions(
    val confused: Int,
    val eyes: Int,
    val heart: Int,
    val hooray: Int,
    val laugh: Int,
    val rocket: Int,
    val total_count: Int,
    val url: String
)
