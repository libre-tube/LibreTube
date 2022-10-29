package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Segment(
    val UUID: String? = null,
    val actionType: String? = null,
    val category: String? = null,
    val description: String? = null,
    val locked: Int? = null,
    val segment: List<Double> = listOf(),
    val userID: String? = null,
    val videoDuration: Double? = null,
    val votes: Int? = null
)
