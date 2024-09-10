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

    /**
     *  Returns a pair of the width and height of the view this listener is used for
     *  These measures change when the screen orientation changes or fullscreen is entered, thus
     *  needs to be refreshed manually all the time when needed.
     */
    fun getViewMeasures(): Pair<Int, Int>
}
