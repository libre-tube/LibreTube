package com.github.libretube.obj

import android.net.Uri
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

    data class User(override val value: String) : ChannelIdentifier

    /**
     * Returns the URL to the channel web page.
     */
    fun url() = when (this) {
        is Id -> "$YOUTUBE_FRONTEND_URL/channel/$value"
        is Name -> "$YOUTUBE_FRONTEND_URL/c/$value"
        is User -> "$YOUTUBE_FRONTEND_URL/user/$value"
        is Handle -> "$YOUTUBE_FRONTEND_URL/@$value"
    }

    companion object {
        /**
         * Creates a channel identifier from a channel URL.
         *
         * Returns null if the URL is not a supported channel URL.
         */
        fun fromUrl(url: Uri) : ChannelIdentifier? {
            val lastSegment = url.lastPathSegment ?: return null
            val secondLastSegment = url.pathSegments.getOrNull(url.pathSegments.size - 2)
            return when {
                secondLastSegment == "channel" -> Id(lastSegment)
                secondLastSegment == "c" -> Name(lastSegment)
                secondLastSegment == "user" -> User(lastSegment)
                lastSegment.startsWith( "@" ) -> Handle(lastSegment.removePrefix("@"))
                else -> null
            }
        }
    }
}