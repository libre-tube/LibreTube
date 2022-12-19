package com.github.libretube.extensions

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.db.obj.LocalPlaylistItem

fun StreamItem.toLocalPlaylistItem(playlistId: String): LocalPlaylistItem {
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
