package com.github.libretube.obj

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
class CustomInstance(
    @ColumnInfo var name: String = "",
    var apiUrl: String = "",
    var frontendUrl: String = ""
)
