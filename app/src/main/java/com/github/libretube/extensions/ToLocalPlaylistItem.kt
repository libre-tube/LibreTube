package com.github.libretube.extensions

import com.github.libretube.api.obj.Streams
import com.github.libretube.db.obj.LocalPlaylistItem

fun Streams.toLocalPlaylistItem(playlistId: String, videoId: String): LocalPlaylistItem {
    return LocalPlaylistItem(
        playlistId = playlistId.toInt(),
        videoId = videoId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploader = uploader,
        uploaderUrl = uploaderUrl,
        uploaderAvatar = uploaderAvatar,
        uploadDate = uploadDate,
        duration = duration
    )
}
