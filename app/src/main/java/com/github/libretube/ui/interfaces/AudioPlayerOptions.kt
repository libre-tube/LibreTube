package com.github.libretube.ui.interfaces

interface AudioPlayerOptions {

    fun onSingleTap()

    fun onLongTap()

    fun onSwipe(distanceY: Float)

    fun onSwipeEnd()
}
