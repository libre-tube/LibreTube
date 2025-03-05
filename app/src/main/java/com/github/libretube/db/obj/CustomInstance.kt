package com.github.libretube.db.obj

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "customInstance")
@Parcelize
class CustomInstance(
    @PrimaryKey var name: String = "",
    @ColumnInfo var apiUrl: String = "",
    @ColumnInfo var frontendUrl: String = ""
) : Parcelable
