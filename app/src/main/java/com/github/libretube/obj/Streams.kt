package com.github.libretube.obj

import android.os.Parcel
import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Streams(
    val title: String?,
    val description: String?,
    val uploadDate: String?,
    val uploader: String?,
    val uploaderUrl: String?,
    val uploaderAvatar: String?,
    val thumbnailUrl: String?,
    val hls: String?,
    val dash: String?,
    val lbryId: String?,
    val uploaderVerified: Boolean?,
    val duration: Int?,
    val views: Long?,
    val likes: Long?,
    val dislikes: Int?,
    val audioStreams: List<PipedStream>?,
    val videoStreams: List<PipedStream>?,
    val relatedStreams: List<StreamItem>?,
    val subtitles: List<Subtitle>?,
    val livestream: Boolean?,
    val proxyUrl: String?,
    val chapters: List<ChapterSegment>?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        TODO("audioStreams"),
        TODO("videoStreams"),
        TODO("relatedStreams"),
        TODO("subtitles"),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readString(),
        TODO("chapters")
    )

    constructor() : this(
        "", "", "", "", "", "", "", "", "", "", null, -1, -1, -1, -1, emptyList(), emptyList(),
        emptyList(), emptyList(), null, "", emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(uploadDate)
        parcel.writeString(uploader)
        parcel.writeString(uploaderUrl)
        parcel.writeString(uploaderAvatar)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(hls)
        parcel.writeString(dash)
        parcel.writeString(lbryId)
        parcel.writeValue(uploaderVerified)
        parcel.writeValue(duration)
        parcel.writeValue(views)
        parcel.writeValue(likes)
        parcel.writeValue(dislikes)
        parcel.writeValue(livestream)
        parcel.writeString(proxyUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Streams> {
        override fun createFromParcel(parcel: Parcel): Streams {
            return Streams(parcel)
        }

        override fun newArray(size: Int): Array<Streams?> {
            return arrayOfNulls(size)
        }
    }
}
