package com.github.libretube.extensions

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.util.ProxyHelper

fun Streams.toStreamItem(videoId: String): StreamItem {
    return StreamItem(
        url = videoId,
        title = title,
        thumbnail = thumbnailUrl,
        uploaderName = uploader,
        uploaderUrl = uploaderUrl,
        uploaderAvatar = uploaderAvatar,
        uploadedDate = uploadDate.toString(),
        uploaded = null,
        duration = duration,
        views = views,
        uploaderVerified = uploaderVerified,
        shortDescription = description
    )
}

fun LocalPlaylistItem.toStreamItem(): StreamItem {
    return StreamItem(
        url = videoId,
        title = title,
        thumbnail = ProxyHelper.rewriteUrl(thumbnailUrl),
        uploaderName = uploader,
        uploaderUrl = uploaderUrl,
        uploaderAvatar = ProxyHelper.rewriteUrl(uploaderAvatar),
        uploadedDate = uploadDate,
        uploaded = null,
        duration = duration
    )
}
