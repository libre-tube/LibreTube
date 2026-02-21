package com.github.libretube.ui.interfaces

interface CustomPlayerCallback {
    fun exitFullscreen()
    fun getVideoId(): String
    fun isVideoShort(): Boolean
}
