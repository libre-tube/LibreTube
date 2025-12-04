package com.github.libretube.ui.controllers

import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import com.github.libretube.extensions.dpToPx
import com.github.libretube.ui.views.CustomExoPlayerView

/**
 * A class that handles the scale animation in [CustomExoPlayerView] when entering/exiting
 * fullscreen using the swipe gesture.
 */
class FullscreenGestureAnimationController(
    private val playerView: CustomExoPlayerView,
    private val onSwipeUpCompleted: () -> Unit,
    private val onSwipeDownCompleted: () -> Unit,
) {
    private var isSwipeInProgress = false
    private var shouldHandleSwipe = false
    /**
     * - `value > 0` = Swiping up
     * - `value < 0` = Swiping down
     */
    private var swipeDirection = 0
    private var swipeStartY = 0f

    fun onSwipe(distanceY: Float, e2: MotionEvent) {
        if (!isSwipeInProgress) {
            isSwipeInProgress = true
            swipeDirection = distanceY.toInt()
            shouldHandleSwipe =
                (!playerView.isFullscreen() && swipeDirection > 0) ||
                        (playerView.isFullscreen() && swipeDirection < 0)
            // Only allow swipe up when not in fullscreen and only allow swipe down when in
            // fullscreen
            if (!shouldHandleSwipe) return

            swipeStartY = e2.y
            playerView.hideController()
            // Set pivot point to the bottom-center
            playerView.pivotX = playerView.width / 2f
            playerView.pivotY = playerView.height.toFloat()
        } else if (shouldHandleSwipe) {
            when {
                swipeDirection > 0 -> {
                    val swipeDistance = (swipeStartY - e2.y).coerceAtLeast(0f)
                    if (swipeDistance >= SWIPE_DISTANCE_THRESHOLD) {
                        onSwipeUpCompleted()
                        return
                    }

                    val scale = 1f + (swipeDistance * SCALE_FACTOR)
                    playerView.scaleX = scale
                    playerView.scaleY = scale
                }

                swipeDirection < 0 -> {
                    val swipeDistance = (e2.y - swipeStartY).coerceAtLeast(0f)
                    if (swipeDistance >= SWIPE_DISTANCE_THRESHOLD) {
                        onSwipeDownCompleted()
                        return
                    }

                    val scale = 1f - (swipeDistance * SCALE_FACTOR)
                    playerView.scaleX = scale
                    playerView.scaleY = scale
                }
            }
        }
    }

    fun onSwipeEnd() {
        if (swipeDirection == 0) return

        // Reset scale
        if (shouldHandleSwipe) playerView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(150)
            .withEndAction {
                // Reset pivot point back to the center
                playerView.pivotY = playerView.height / 2f
                playerView.pivotX = playerView.width / 2f
            }
            .start()

        isSwipeInProgress = false
        swipeDirection = 0
    }

    companion object {
        private const val SCALE_DIFF_TARGET = 0.12f
        private val SWIPE_DISTANCE_THRESHOLD = 50f.dpToPx()
        private val SCALE_FACTOR = SCALE_DIFF_TARGET / SWIPE_DISTANCE_THRESHOLD
    }
}