package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "customInstance")
class CustomInstance(
    @PrimaryKey var name: String = "",
    @ColumnInfo var apiUrl: String = "",
    @ColumnInfo var frontendUrl: String = ""
)
