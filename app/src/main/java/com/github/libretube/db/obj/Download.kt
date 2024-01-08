package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.nio.file.Path
import kotlinx.datetime.LocalDate

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
    val thumbnailPath: Path? = null
)
