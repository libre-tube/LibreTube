package com.github.libretube.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoStats(
    val videoId: String,
    val videoInfo: String,
    val videoQuality: String,
    val audioInfo: String
) : Parcelable
