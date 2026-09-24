package com.github.libretube.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShareData(
    val title: String?,
    val currentPosition: Long? = null,
    val previewImageUrl: String? = null
) : Parcelable
