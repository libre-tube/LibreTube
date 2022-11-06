package com.github.libretube.extensions

/**
 * format a Piped route to an ID
 */
fun String.toID(): String {
    return this
        .replace("/watch?v=", "") // videos
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
}
