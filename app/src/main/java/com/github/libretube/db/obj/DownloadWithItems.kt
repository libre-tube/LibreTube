package com.github.libretube.db.obj

import androidx.room.Embedded
import androidx.room.Relation

data class DownloadWithItems(
    @Embedded val download: Download,
    @Relation(
        parentColumn = "videoId",
        entityColumn = "videoId"
    )
    val downloadItems: List<DownloadItem>
)
