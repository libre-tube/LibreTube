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

    // reminder: all the attributed that are asserted as non-null here are declared non-null by the
    // SponsorBlock API, so it's safe to assert they're not null (if we trust the SponsorBlock API docs)
    fun toDownloadSegment(videoId: String): DownloadSponsorBlockSegment = DownloadSponsorBlockSegment(
        uuid = uuid!!,
        videoId = videoId,
        startTime = segmentStartAndEnd.first,
        endTime = segmentStartAndEnd.second,
        actionType = actionType!!,
        category = category!!,
        description = description,
        locked = locked!!,
        videoDuration = videoDuration!!.toFloat(),
        votes = votes!!
    )

    companion object {
        const val TYPE_FULL = "full"
    }
}
