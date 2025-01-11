package com.github.libretube.api.obj

import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.helpers.ProxyHelper
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val name: String? = null,
    var description: String? = null,
    val thumbnailUrl: String? = null,
    val bannerUrl: String? = null,
    val nextpage: String? = null,
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val videos: Int = 0,
    var relatedStreams: List<StreamItem> = emptyList()
) {
    fun toPlaylistBookmark(playlistId: String): PlaylistBookmark {
        return PlaylistBookmark(
            playlistId = playlistId,
            playlistName = name,
            thumbnailUrl = thumbnailUrl?.let { ProxyHelper.unwrapUrl(it) },
            uploader = uploader,
            uploaderAvatar = uploaderAvatar?.let { ProxyHelper.unwrapUrl(it) },
            uploaderUrl = uploaderUrl,
            videos = videos
        )
    }
}
