package com.github.libretube.api.obj

import android.os.Parcelable
import androidx.collection.FloatFloatPair
import com.github.libretube.db.obj.DownloadSponsorBlockSegment
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// see https://wiki.sponsor.ajay.app/w/API_Docs#GET_/api/skipSegments
@Serializable
@Parcelize
data class Segment(
    @SerialName("UUID") val uuid: String,
    val actionType: String,
    val category: String,
    val description: String? = null,
    val locked: Int,
    private val segment: List<Float> = listOf(),
    val userID: String? = null,
    val videoDuration: Double,
    val votes: Int,
    var skipped: Boolean = false
): Parcelable {
    @Transient
    @IgnoredOnParcel
    val segmentStartAndEnd = FloatFloatPair(segment[0], segment[1])

    fun toDownloadSegment(videoId: String): DownloadSponsorBlockSegment = DownloadSponsorBlockSegment(
        uuid = uuid,
        videoId = videoId,
        startTime = segmentStartAndEnd.first,
        endTime = segmentStartAndEnd.second,
        actionType = actionType,
        category = category,
        description = description,
        locked = locked,
        videoDuration = videoDuration.toFloat(),
        votes = votes
    )

    companion object {
        const val TYPE_FULL = "full"
    }
}
