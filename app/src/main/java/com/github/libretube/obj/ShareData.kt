package com.github.libretube.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShareData(
    val title: String?,
    var currentPosition: Long? = null
) : Parcelable
