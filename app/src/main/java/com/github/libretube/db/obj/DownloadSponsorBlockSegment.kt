package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.github.libretube.api.obj.Segment

@Entity(
    tableName = "downloadSponsorBlockSegment",
    foreignKeys = [
        ForeignKey(
            entity = Download::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadSponsorBlockSegment(
    @PrimaryKey
    val uuid: String,
    val videoId: String,

    val actionType: String,
    val category: String,
    val description: String? = null,
    val locked: Int,
    val startTime: Float,
    val endTime: Float,
    val videoDuration: Float,
    val votes: Int,
) {
    fun toSegment(): Segment = Segment(
        uuid = uuid,
        segment = listOf(startTime, endTime),
        actionType = actionType,
        category = category,
        description = description,
        locked = locked,
        videoDuration = videoDuration.toDouble(),
        votes = votes
    )
}