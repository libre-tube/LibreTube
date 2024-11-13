package com.github.libretube.api.obj

import android.os.Parcelable
import androidx.collection.FloatFloatPair
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Parcelize
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
): Parcelable {
    @Transient
    @IgnoredOnParcel
    val segmentStartAndEnd = FloatFloatPair(segment[0], segment[1])

    companion object {
        const val TYPE_FULL = "full"
    }
}
