package com.github.libretube.ui.interfaces

interface CustomPlayerCallback {
    fun toggleFullscreen()
    fun getVideoId(): String
    fun isVideoShort(): Boolean
}
