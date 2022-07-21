package com.github.libretube

import android.content.Intent

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

    // background mode intent
    var backgroundModeIntent: Intent? = null
}
