package com.github.libretube.ui.listeners

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.activity.viewModels
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.interfaces.PlayerGestureOptions
import com.github.libretube.ui.models.CommonPlayerViewModel
import java.time.Instant
import kotlin.math.abs

class PlayerGestureController(activity: BaseActivity, private val listener: PlayerGestureOptions) {

    private val orientation get() = Resources.getSystem().configuration.orientation

    private val commonPlayerViewModel: CommonPlayerViewModel by activity.viewModels()
    private val handler = Handler(Looper.getMainLooper())

    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private val doubleTapSlop = ViewConfiguration.get(activity).scaledDoubleTapSlop

    private var isFullscreen = false
    private var scaleGestureWasInProgress = false
    private var isMoving = false
    var longPressInProgress = false
    var lastDoublePressTime: Instant? = null

    var areControlsLocked = false

    init {
        gestureDetector = GestureDetector(activity, GestureListener(), handler)
        scaleGestureDetector = ScaleGestureDetector(activity, ScaleGestureListener(), handler)

        commonPlayerViewModel.isFullscreen.observe(activity) {
            isFullscreen = it
            listener.onFullscreenChange(it)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleGestureWasInProgress = false

                val (_, height) = listener.getViewMeasures()
                if (event.y < height * 0.1f && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // when in landscape mode, don't consume this event if touch down area is at the
                    // top of the player
                    return false
                }

                if (areControlsLocked) {
                    // notify the listener that the player controls are currently locked
                    listener.onSingleTap(true)

                    // controls locked, no need to consume this event
                    return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureWasInProgress && scaleGestureDetector.isInProgress) {
                    scaleGestureWasInProgress = true

                    // scale gesture is triggered, cancel any other ongoing gesture detections
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL,
                        event.x,
                        event.y,
                        event.metaState
                    ).also { cancelEvent ->
                        gestureDetector.onTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                    }
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (isMoving) listener.onSwipeEnd()
                isMoving = false

                if (longPressInProgress) listener.onLongPressEnd()
                longPressInProgress = false
            }
        }

        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureWasInProgress) gestureDetector.onTouchEvent(event)

        return true
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        var scaleFactor = 1f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
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
        private var touchGestureDownX = 0f
        private var touchGestureDownY = 0f

        override fun onDown(e: MotionEvent): Boolean {
            touchGestureDownX = e.x
            touchGestureDownY = e.y

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            longPressInProgress = true
            listener.onLongPress()
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_UP -> {
                    if (
                        abs(e.y - touchGestureDownY) > doubleTapSlop ||
                        abs(e.x - touchGestureDownX) > doubleTapSlop
                    ) {
                        // not considered a double tap
                        return false
                    }

                    val (width, _) = listener.getViewMeasures()
                    val eventPositionPercentageX = e.x / width

                    when {
                        eventPositionPercentageX < LEFT_AREA_VIEW_PERCENTAGE -> {
                            listener.onDoubleTapLeftScreen()
                        }

                        eventPositionPercentageX > RIGHT_AREA_VIEW_PERCENTAGE -> {
                            listener.onDoubleTapRightScreen()
                        }

                        else -> listener.onDoubleTapCenterScreen()
                    }
                }
            }

            lastDoublePressTime = Instant.now()

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // if there has been a recent double click within the threshold, single clicks
            // are being treated as a repetition of the double click
            // e.g. this allows to continue seeking forward using single clicks after double-clicking once
            lastDoublePressTime?.takeIf {
                val millisElapsedSinceDoubleClick = Instant.now().toEpochMilli() - it.toEpochMilli()
                millisElapsedSinceDoubleClick < DOUBLE_TAP_REPEAT_TIME_THRESHOLD_MILLIS
            }?.let {
                val upEvent = MotionEvent.obtain(e).apply {
                    action = MotionEvent.ACTION_UP
                }
                onDoubleTapEvent(upEvent)
                upEvent.recycle()

                lastDoublePressTime = Instant.now()
                return true
            }

            listener.onSingleTap(false)

            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val (width, height) = listener.getViewMeasures()
            val insideThreshHold = abs(e2.y - e1!!.y) <= MOVEMENT_THRESHOLD
            val insideBorder =
                (e1.x < BORDER_THRESHOLD || e1.y < BORDER_THRESHOLD || e1.x > width - BORDER_THRESHOLD || e1.y > height - BORDER_THRESHOLD)

            // If the movement is inside threshold or scroll is horizontal then return false
            if (!isMoving && (insideThreshHold || insideBorder || abs(distanceX) > abs(distanceY))) {
                return false
            }

            isMoving = true

            if (!isFullscreen && distanceY > 0) {
                // Allow swipe up on the entire area if the player is not currently in fullscreen
                listener.onSwipeCenterScreen(distanceY, e2.y)
                return true
            }

            when {
                e1.x < width * LEFT_AREA_VIEW_PERCENTAGE -> {
                    if (isFullscreen) listener.onSwipeLeftScreen(distanceY, e2.y)
                }

                e1.x > width * RIGHT_AREA_VIEW_PERCENTAGE -> {
                    if (isFullscreen) listener.onSwipeRightScreen(distanceY, e2.y)
                }

                else -> listener.onSwipeCenterScreen(distanceY, e2.y)
            }

            return true
        }
    }

    companion object {
        private const val MOVEMENT_THRESHOLD = 30
        private const val BORDER_THRESHOLD = 90
        private const val LEFT_AREA_VIEW_PERCENTAGE = 0.35f
        private const val RIGHT_AREA_VIEW_PERCENTAGE = 0.65f

        /**
         * After a double click was processed, the user may use single clicks to trigger another double click.
         */
        private const val DOUBLE_TAP_REPEAT_TIME_THRESHOLD_MILLIS = 700
    }
}
