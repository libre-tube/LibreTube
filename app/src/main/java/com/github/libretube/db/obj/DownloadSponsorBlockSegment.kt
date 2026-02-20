package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
)