package com.github.libretube.obj

import android.net.Uri
import android.os.Parcelable
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

/**
 *  Represents a YouTube channel identifier, such as the channel ID.
 *
 * For more information see the [official YouTube documentation](https://support.google.com/youtube/answer/6180214).
 */
@Serializable
@Parcelize
sealed interface ChannelIdentifier: Parcelable {
    val value: String

    /** A channel identified by its unique channel ID. */
    data class Id(override val value: String) : ChannelIdentifier

    /** A channel identified by its custom name. */
    data class Custom(override val value: String) : ChannelIdentifier

    /** A channel identified by its user handle. */
    data class Handle(override val value: String) : ChannelIdentifier

    /** A channel identified by a `/user/name` URL. */
    data class User(override val value: String) : ChannelIdentifier

    /**
     * Returns the URL to the channel web page.
     */
    fun url() = when (this) {
        is Id -> "$YOUTUBE_FRONTEND_URL/channel/$value"
        is Custom -> "$YOUTUBE_FRONTEND_URL/c/$value"
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
                secondLastSegment == "c" -> Custom(lastSegment)
                secondLastSegment == "user" -> User(lastSegment)
                lastSegment.startsWith("@") -> Handle(lastSegment.removePrefix("@"))
                else -> null
            }
        }
    }
}