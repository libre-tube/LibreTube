package com.github.libretube.api.obj

import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.extensions.toID
import kotlinx.serialization.Serializable

@Serializable
data class StreamItem(
    val url: String? = null,
    val type: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean? = null,
    val uploaded: Long? = null,
    val shortDescription: String? = null,
    val isShort: Boolean = false
) {
    fun toLocalPlaylistItem(playlistId: String): LocalPlaylistItem {
        return LocalPlaylistItem(
            playlistId = playlistId.toInt(),
            videoId = url!!.toID(),
            title = title,
            thumbnailUrl = thumbnail,
            uploader = uploaderName,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = uploaderAvatar,
            uploadDate = uploadedDate,
            duration = duration
        )
    }
}
