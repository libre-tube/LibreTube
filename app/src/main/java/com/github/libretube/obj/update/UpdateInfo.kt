package com.github.libretube.obj.update

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UpdateInfo(
    val name: String, // version name
    val body: String, // changelog
    val html_url: String // uri to latest release tag
) : Parcelable
