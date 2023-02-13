package com.github.libretube.ui.interfaces

interface PlayerGestureOptions {

    fun onSingleTap()

    fun onDoubleTapCenterScreen()

    fun onDoubleTapLeftScreen()

    fun onDoubleTapRightScreen()

    fun onSwipeLeftScreen(distanceY: Float)

    fun onSwipeRightScreen(distanceY: Float)

    fun onSwipeCenterScreen(distanceY: Float)

    fun onSwipeEnd()

    fun onZoom()

    fun onMinimize()

    fun onFullscreenChange(isFullscreen: Boolean)
}
