package com.github.libretube.util

/**
 * format a Piped route to an ID
 */
fun Any?.toID(): String {
    return this!!
        .toString()
        .replace("/watch?v=", "") // videos
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
}
