package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.libretube.enums.FileType
import java.nio.file.Path
import kotlin.io.path.fileSize

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
    var path: Path,
    var format: String? = null,
    var quality: String? = null,
    var language: String? = null,
    /**
     * Total size of the item in bytes (not the current download progress!).
     */
    var downloadSize: Long = -1L,
    /**
     * Current download progress of the video in milliseconds. Only used for SABR downloads.
     */
    var currentDownloadPositionMillis: Long? = null
) {
    val isFinished get() = runCatching { path.fileSize() }.getOrDefault(0L) < downloadSize
}
