package com.github.libretube.api.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class MetaInfo(
    val title: String,
    val description: String,
    val urls: List<String>,
    val urlTexts: List<String>
): Parcelable
