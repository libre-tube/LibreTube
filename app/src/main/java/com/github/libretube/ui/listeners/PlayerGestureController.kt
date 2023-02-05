package com.github.libretube.ui.listeners

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.activity.viewModels
import androidx.core.os.postDelayed
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.interfaces.PlayerGestureOptions
import com.github.libretube.ui.models.PlayerViewModel
import kotlin.math.abs

class PlayerGestureController(activity: BaseActivity, private val listener: PlayerGestureOptions) :
    View.OnTouchListener {

    // width and height should be obtained each time using getter to adopt layout
    // size changes.
    private val width get() = Resources.getSystem().displayMetrics.widthPixels
    private val height get() = Resources.getSystem().displayMetrics.heightPixels
    private val orientation get() = Resources.getSystem().configuration.orientation
    private val elapsedTime get() = SystemClock.elapsedRealtime()

    private val playerViewModel: PlayerViewModel by activity.viewModels()
    private val handler = Handler(Looper.getMainLooper())

    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    private var isFullscreen = false
    private var isMoving = false
    var isEnabled = true

    // Indicates last touch event was for click or other gesture, used to avoid single click
    // by runnable when scroll or pinch gesture already completed.
    var wasClick = true

    init {
        gestureDetector = GestureDetector(activity, GestureListener(), handler)
        scaleGestureDetector = ScaleGestureDetector(activity, ScaleGestureListener(), handler)

        playerViewModel.isFullscreen.observe(activity) {
            isFullscreen = it
            listener.onFullscreenChange(it)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && isMoving) {
            isMoving = false
            listener.onSwipeEnd()
        }

        // ignore touches to the top of the player when in landscape mode
        if (event.y < height * 0.1 && orientation == Configuration.ORIENTATION_LANDSCAPE) return false

        // Event can be already consumed by some view which may lead to NPE.
        try {
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
        } catch (_: Exception) {
        }

        // If video is playing in full-screen mode, then allow `onScroll` to consume
        // event and return true.
        return isFullscreen
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        var scaleFactor: Float = 1f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            wasClick = false
            scaleFactor *= detector.scaleFactor
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            when {
                scaleFactor < 0.8 -> listener.onMinimize()
                scaleFactor > 1.2 -> listener.onZoom()
            }
            scaleFactor = 1f
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private var lastClick = 0L
        private var lastDoubleClick = 0L

        override fun onDown(e: MotionEvent): Boolean {
            // Initially assume this event is for click
            wasClick = true

            if (isMoving || scaleGestureDetector.isInProgress) return false

            if (!PlayerHelper.doubleTapToSeek) {
                listener.onSingleTap()
                return true
            }

            if (isEnabled && isSecondClick()) {
                handler.removeCallbacksAndMessages(SINGLE_TAP_TOKEN)
                lastDoubleClick = elapsedTime
                val eventPositionPercentageX = e.x / width

                when {
                    eventPositionPercentageX < 0.4 -> listener.onDoubleTapLeftScreen()
                    eventPositionPercentageX > 0.6 -> listener.onDoubleTapRightScreen()
                    else -> listener.onDoubleTapCenterScreen()
                }
            } else {
                if (recentDoubleClick()) return true
                handler.removeCallbacksAndMessages(SINGLE_TAP_TOKEN)
                handler.postDelayed(MAX_TIME_DIFF, SINGLE_TAP_TOKEN) {
                    // If the last event was for scroll or pinch then avoid single tap call
                    if (!wasClick || isSecondClick()) return@postDelayed
                    listener.onSingleTap()
                }
                lastClick = elapsedTime
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!isEnabled || scaleGestureDetector.isInProgress) return false

            val insideThreshHold = abs(e2.y - e1.y) <= MOVEMENT_THRESHOLD
            val insideBorder =
                (e1.x < BORDER_THRESHOLD || e1.y < BORDER_THRESHOLD || e1.x > width - BORDER_THRESHOLD || e1.y > height - BORDER_THRESHOLD)

            // If the movement is inside threshold or scroll is horizontal then return false
            if (!isMoving && (insideThreshHold || insideBorder || abs(distanceX) > abs(distanceY))) {
                return false
            }

            isMoving = true
            wasClick = false

            when {
                width * 0.5 > e1.x -> listener.onSwipeLeftScreen(distanceY)
                width * 0.5 < e1.x -> listener.onSwipeRightScreen(distanceY)
            }
            return true
        }

        private fun isSecondClick(): Boolean {
            return elapsedTime - lastClick < MAX_TIME_DIFF
        }

        private fun recentDoubleClick(): Boolean {
            return elapsedTime - lastDoubleClick < MAX_TIME_DIFF / 2
        }
    }

    companion object {
        private const val SINGLE_TAP_TOKEN = "singleTap"

        private const val MAX_TIME_DIFF = 400L
        private const val MOVEMENT_THRESHOLD = 30
        private const val BORDER_THRESHOLD = 90
    }
}
