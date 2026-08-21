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
data class CustomInstance(
    @PrimaryKey val name: String = "",
    @ColumnInfo val apiUrl: String = "",
    @ColumnInfo val frontendUrl: String = ""
) : Parcelable
