package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Segment(
    val UUID: String? = null,
    val actionType: String? = null,
    val category: String? = null,
    val description: String? = null,
    val locked: Int? = null,
    val segment: List<Double> = listOf(),
    val userID: String? = null,
    val videoDuration: Double? = null,
    val votes: Int? = null,
    var skipped: Boolean = false
) {
    val segmentStartAndEnd = segment[0] to segment[1]
}
