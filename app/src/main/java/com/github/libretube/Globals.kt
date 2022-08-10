package com.github.libretube

/**
 * Global variables can be stored here
 */
object Globals {
    // for the player fragment
    var IS_FULL_SCREEN = false
    var MINI_PLAYER_VISIBLE = false

    // for the data saver mode
    var DATA_SAVER_MODE_ENABLED = false

    // for downloads
    var IS_DOWNLOAD_RUNNING = false

    // for playlists
    var SELECTED_PLAYLIST_ID: String? = null

    // history of played videos in the current lifecycle
    val playingQueue = mutableListOf<String>()
}
