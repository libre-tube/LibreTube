package com.github.libretube.extensions

import com.github.libretube.constants.YouTubeConstants

/**
 * format a full YouTube url or a path to a video/channel/playlist ID
 */
fun String.toID(): String {
    return this
        .removePrefix(YouTubeConstants.FRONTEND_URL)
        .removePrefix(YouTubeConstants.MUSIC_URL)
        .removePrefix(YouTubeConstants.SHORT_URL)
        .replace("/watch?v=", "")
        .replace("/channel/", "")
        .replace("/playlist?list=", "")
        .removeSuffix("/shorts")
        .removeSuffix("/streams")
        .removeSuffix("/videos")
}
