package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "searchHistoryItem")
data class SearchHistoryItem(
    @PrimaryKey val query: String = ""
)
