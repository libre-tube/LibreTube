package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toMillis
import kotlinx.datetime.LocalDate
import java.nio.file.Path

@Entity(tableName = "download")
data class Download(
    @PrimaryKey(autoGenerate = false)
    val videoId: String,
    val title: String = "",
    val description: String = "",
    val uploader: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val duration: Long? = null,
    val uploadDate: LocalDate? = null,
    val thumbnailPath: Path? = null,
    val uploaderUrl: String? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = -1,
) {
    fun toStreamItem() = StreamItem(
        url = videoId,
        title = title,
        shortDescription = description,
        thumbnail = thumbnailPath?.toUri()?.toString(),
        duration = duration,
        uploaded = uploadDate?.toMillis() ?: 0L,
        uploadedDate = uploadDate?.toString(),
        uploaderName = uploader,
        uploaderUrl = uploaderUrl,
        views = views,
    )
}
