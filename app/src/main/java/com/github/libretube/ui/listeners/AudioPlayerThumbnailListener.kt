package com.github.libretube.ui.listeners

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.os.postDelayed
import com.github.libretube.ui.interfaces.AudioPlayerOptions
import kotlin.math.abs

class AudioPlayerThumbnailListener(context: Context, private val listener: AudioPlayerOptions) :
    View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private val gestureDetector = GestureDetector(context, GestureListener(), handler)
    private var isMoving = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && isMoving) {
            isMoving = false
            listener.onSwipeEnd()
            return false
        }

        runCatching {
            gestureDetector.onTouchEvent(event)
        }

        return true
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            // Initially assume this event is for click
            if (isMoving) return false

            handler.postDelayed(300) {
                if (!isMoving) listener.onSingleTap()
            }

            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val insideThreshHold = abs(e2.y - e1.y) <= MOVEMENT_THRESHOLD

            // If the movement is inside threshold or scroll is horizontal then return false
            if (!isMoving && (insideThreshHold || abs(distanceX) > abs(distanceY))) {
                return false
            }

            isMoving = true

            listener.onSwipe(distanceY)
            return true
        }
    }

    companion object {
        private const val MOVEMENT_THRESHOLD = 10
    }
}
