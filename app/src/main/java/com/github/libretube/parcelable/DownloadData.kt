package com.github.libretube.parcelable

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadData(
    val videoId: String,
    val fileName: String,
    val videoFormat: String?,
    val videoQuality: String?,
    val audioFormat: String?,
    val audioQuality: String?,
    val subtitleCode: String?
) : Parcelable
