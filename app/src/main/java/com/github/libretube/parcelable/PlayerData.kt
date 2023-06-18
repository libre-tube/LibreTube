package com.github.libretube.parcelable

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerData(
    val videoId: String,
    val playlistId: String? = null,
    val channelId: String? = null,
    val keepQueue: Boolean = false,
    val timestamp: Long = 0
) : Parcelable
