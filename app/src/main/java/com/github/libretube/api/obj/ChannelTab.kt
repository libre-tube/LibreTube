package com.github.libretube.api.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ChannelTab(
    val name: String,
    val data: String
) : Parcelable
