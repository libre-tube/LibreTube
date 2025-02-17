package com.github.libretube.extensions

import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL

/**
 * format a Piped route to an ID
 */
fun String.toID(): String {
    return this
        .replace(YOUTUBE_FRONTEND_URL, "")
        .replace("/watch?v=", "") // videos
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
        // channel urls for different categories than the main one
        .removeSuffix("/shorts")
        .removeSuffix("/streams")
        .removeSuffix("/videos")
}
