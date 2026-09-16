package com.github.libretube.db.obj

import androidx.room.ColumnInfo
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
    var currentDownloadPositionMillis: Long? = null,
    /**
     * Next SABR media segment to request. Used to resume without rewriting the init segment.
     * Non-null while a SABR download is in progress; cleared once the file is verified.
     */
    @ColumnInfo(defaultValue = "NULL")
    var currentSegmentNumber: Long? = null
) {
    // currentSegmentNumber doubles as the in-progress marker, since the SABR byte count
    // (contentLength from the manifest) is not guaranteed to match what the SABR server serves.
    val isFinished
        get() = currentSegmentNumber == null && downloadSize > 0L &&
            runCatching { path.fileSize() }.getOrDefault(0L) >= downloadSize
}
