package com.github.libretube.ui.controllers

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.github.libretube.extensions.dpToPx
import com.github.libretube.ui.views.CustomExoPlayerView

/**
 * A class that handles the scale animation in [CustomExoPlayerView] when entering/exiting
 * fullscreen using the swipe gesture.
 */
class FullscreenGestureAnimationController(
    private val playerView: CustomExoPlayerView,
    private val videoFrameView: View,
    private val onSwipeUpCompleted: () -> Unit,
    private val onSwipeDownCompleted: () -> Unit,
) {
    enum class SwipeDirection { UP, DOWN, NONE }

    private var isSwipeInProgress = false
    private var isSwipeCompleted = false
    private var shouldHandleSwipe = false
    private var swipeDirection = SwipeDirection.NONE
    private var swipeStartY = 0f

    fun onSwipe(distanceY: Float, positionY: Float) {
        if (!isSwipeInProgress) {
            isSwipeInProgress = true
            swipeDirection = if (distanceY.toInt() > 0) SwipeDirection.UP else SwipeDirection.DOWN
            shouldHandleSwipe =
                if (!playerView.isFullscreen()) swipeDirection == SwipeDirection.UP
                else swipeDirection == SwipeDirection.DOWN

            // Only allow swipe up when not in fullscreen and only allow swipe down when in
            // fullscreen
            if (!shouldHandleSwipe) return

            swipeStartY = positionY
            playerView.hideController()
            // Set pivot point to the bottom-center
            videoFrameView.pivotX = videoFrameView.width / 2f
            videoFrameView.pivotY = videoFrameView.height.toFloat()
        } else if (shouldHandleSwipe && !isSwipeCompleted) {
            when (swipeDirection) {
                SwipeDirection.UP -> {
                    val swipeDistance = (swipeStartY - positionY).coerceAtLeast(0f)
                    if (swipeDistance >= SWIPE_DISTANCE_THRESHOLD ) {
                        isSwipeCompleted = true
                        onSwipeUpCompleted()
                        return
                    }

                    val scale = 1f + (swipeDistance * SCALE_FACTOR)
                    videoFrameView.scaleX = scale
                    videoFrameView.scaleY = scale
                }

                SwipeDirection.DOWN -> {
                    val swipeDistance = (positionY - swipeStartY).coerceAtLeast(0f)
                    if (swipeDistance >= SWIPE_DISTANCE_THRESHOLD) {
                        isSwipeCompleted = true
                        onSwipeDownCompleted()
                        return
                    }

                    val scale = 1f - (swipeDistance * SCALE_FACTOR)
                    videoFrameView.scaleX = scale
                    videoFrameView.scaleY = scale
                }

                // Do nothing
                SwipeDirection.NONE -> {}
            }
        }
    }

    fun onSwipeEnd() {
        if (swipeDirection == SwipeDirection.NONE) return

        // Reset scale
        if (shouldHandleSwipe) videoFrameView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(150)
            .withEndAction {
                // Reset pivot point back to the center
                videoFrameView.pivotY = videoFrameView.height / 2f
                videoFrameView.pivotX = videoFrameView.width / 2f
            }
            .start()

        isSwipeInProgress = false
        isSwipeCompleted = false
        swipeDirection = SwipeDirection.NONE
    }

    companion object {
        /**
         * The amount of percentage the view will be scaled up and down.
         */
        private const val MAXIMUM_SCALE_DIFF_PERCENTAGE = 0.12f
        private val SWIPE_DISTANCE_THRESHOLD = 50f.dpToPx()
        private val SCALE_FACTOR = MAXIMUM_SCALE_DIFF_PERCENTAGE / SWIPE_DISTANCE_THRESHOLD
    }
}