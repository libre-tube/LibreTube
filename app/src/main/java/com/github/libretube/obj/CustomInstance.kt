package com.github.libretube.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customInstance")
class CustomInstance(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo var name: String = "",
    @ColumnInfo var apiUrl: String = "",
    @ColumnInfo var frontendUrl: String = ""
)
