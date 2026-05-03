package com.github.libretube.api.ltsync.obj


import com.github.libretube.db.obj.PlaylistBookmark
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Public (API) view of a read-only playlist (e.g. from YouTube).
 *
 * @param playlist
 * @param uploader
 */
@Serializable
data class ExtendedPublicPlaylist (
    @SerialName(value = "playlist")
    val playlist: ExtendedPlaylist,

    @SerialName(value = "uploader")
    val uploader: Channel
) {
    fun toPlaylistBookmark(): PlaylistBookmark {
        return PlaylistBookmark(
            playlistName = playlist.title,
            thumbnailUrl = playlist.thumbnailUrl,
            uploader = uploader.name,
            uploaderUrl = uploader.id,
            uploaderAvatar = uploader.avatar,
            videos = playlist.videoCount?.toInt() ?: -1
        )
    }
}

