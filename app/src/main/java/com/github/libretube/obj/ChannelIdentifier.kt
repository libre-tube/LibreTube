package com.github.libretube.obj

import android.os.Parcelable
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
sealed interface ChannelIdentifier: Parcelable {
    val value: String

    data class Id(override val value: String) : ChannelIdentifier

    data class Name(override val value: String) : ChannelIdentifier

    data class Handle(override val value: String) : ChannelIdentifier

    /**
     * Returns the URL to the channel web page.
     */
    fun url() = when (this) {
        is Id -> "$YOUTUBE_FRONTEND_URL/channel/$value"
        is Name -> "$YOUTUBE_FRONTEND_URL/c/$value"
        is Handle -> "$YOUTUBE_FRONTEND_URL/@$value"
    }
}