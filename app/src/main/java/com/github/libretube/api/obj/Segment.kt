package com.github.libretube.api.obj

import androidx.collection.FloatFloatPair
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Segment(
    @SerialName("UUID") val uuid: String? = null,
    val actionType: String? = null,
    val category: String? = null,
    val description: String? = null,
    val locked: Int? = null,
    private val segment: List<Float> = listOf(),
    val userID: String? = null,
    val videoDuration: Double? = null,
    val votes: Int? = null,
    var skipped: Boolean = false
) {
    @Transient
    val segmentStartAndEnd = FloatFloatPair(segment[0], segment[1])
}
