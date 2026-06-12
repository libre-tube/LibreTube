package com.github.libretube.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ShareObjectType: Parcelable {
    VIDEO,
    PLAYLIST,
    CHANNEL
}
