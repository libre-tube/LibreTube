package com.github.libretube.extensions

import android.net.Uri
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_MUSIC_URL
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_SHORT_URL

/**
 * format a full YouTube url or a path to a video/channel/playlist ID
 */
fun String.toID(): String {
    val clean = this
        .removeSuffix("/shorts")
        .removeSuffix("/streams")
        .removeSuffix("/videos")

    if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("/")) {
        try {
            val fullUrl = if (clean.startsWith("/")) "https://www.youtube.com$clean" else clean
            val uri = Uri.parse(fullUrl)
            val v = uri.getQueryParameter("v")
            if (!v.isNullOrEmpty()) return v

            val list = uri.getQueryParameter("list")
            if (!list.isNullOrEmpty()) return list

            val last = uri.lastPathSegment
            if (!last.isNullOrEmpty() && (
                uri.host == "youtu.be" ||
                uri.path?.contains("/shorts/") == true ||
                uri.path?.contains("/channel/") == true ||
                uri.path?.contains("/c/") == true ||
                uri.path?.contains("/user/") == true ||
                last.startsWith("@")
            )) {
                return last
            }
        } catch (_: Exception) {}
    }

    return clean
        // remove any youtube origins from urls
        .removePrefix(YOUTUBE_FRONTEND_URL)
        .removePrefix(YOUTUBE_MUSIC_URL)
        .removePrefix(YOUTUBE_SHORT_URL)
        .removePrefix("https://youtube.com")
        .removePrefix("http://youtube.com")
        .removePrefix("http://www.youtube.com")
        .removePrefix("https://m.youtube.com")
        .removePrefix("http://m.youtube.com")
        .replace("/watch?v=", "") // videos
        .replace("/shorts/", "") // shorts
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
}
