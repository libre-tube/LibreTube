package com.github.libretube.ui.interfaces

import android.view.MotionEvent

interface PlayerGestureOptions {

    fun onSingleTap(areControlsLocked: Boolean)

    fun onDoubleTapCenterScreen()

    fun onDoubleTapLeftScreen()

    fun onDoubleTapRightScreen()

    fun onSwipeLeftScreen(distanceY: Float, e2: MotionEvent)

    fun onSwipeRightScreen(distanceY: Float, e2: MotionEvent)

    fun onSwipeCenterScreen(distanceY: Float, e2: MotionEvent)

    fun onSwipeEnd()

    fun onZoom()

    fun onMinimize()

    fun onFullscreenChange(isFullscreen: Boolean)

    fun onLongPress()

    fun onLongPressEnd()

    /**
     *  Returns a pair of the width and height of the view this listener is used for
     *  These measures change when the screen orientation changes or fullscreen is entered, thus
     *  needs to be refreshed manually all the time when needed.
     */
    fun getViewMeasures(): Pair<Int, Int>
}
