package com.github.libretube.obj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "searchHistoryItem")
data class SearchHistoryItem(
    @PrimaryKey val query: String = ""
)
