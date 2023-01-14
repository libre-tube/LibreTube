package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.libretube.enums.FileType

@Entity(
    tableName = "downloadItem",
    indices = [Index(value = ["path"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Download::class,
            parentColumns = ["videoId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val type: FileType,
    val videoId: String,
    val fileName: String,
    var path: String,
    var url: String? = null,
    var format: String? = null,
    var quality: String? = null,
    var downloadSize: Long = -1L
)
