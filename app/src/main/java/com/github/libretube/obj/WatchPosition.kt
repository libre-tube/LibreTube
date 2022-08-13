package com.github.libretube.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchPosition")
data class WatchPosition(
    @PrimaryKey val videoId: String = "",
    @ColumnInfo val position: Long = 0L
)
