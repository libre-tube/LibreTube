package com.github.libretube.obj.update

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UpdateInfo(
    val name: String,
    val body: String,
    @SerialName("html_url") val htmlUrl: String
) : Parcelable
