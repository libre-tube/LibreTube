package com.github.libretube

/**
 * Global variables can be stored here
 */
object Globals {
    // for downloads
    var IS_DOWNLOAD_RUNNING = false

    // history of played videos in the current lifecycle
    val playingQueue = mutableListOf<String>()
}
