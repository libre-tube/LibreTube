package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LocalPlaylist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val thumbnailUrl: String
)
