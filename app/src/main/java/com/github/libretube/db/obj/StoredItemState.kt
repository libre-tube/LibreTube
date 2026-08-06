package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class StoredItemState(
   val videoId: String = "",
   val title: String? = null,
   val uploader: String? = null,
   val uploaderUrl: String? = null,
)
