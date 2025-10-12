package com.github.libretube.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.TimeBar
import androidx.media3.ui.TimeBar.OnScrubListener
import com.github.libretube.extensions.dpToPx
import kotlin.math.abs

@UnstableApi
open class DismissableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
): DefaultTimeBar(context, attributeSet) {
    private var shouldAddListener = false
    var exoPlayer: Player? = null
    private var lastYPosition = 0f

    init {
        addSeekBarListener(object : OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) = Unit

            override fun onScrubMove(timeBar: TimeBar, position: Long) = Unit

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                if (lastYPosition > MINIMUM_ACCEPTED_HEIGHT.dpToPx()) exoPlayer?.seekTo(position)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastYPosition = event.y

        val exoPlayer = exoPlayer ?: return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val duration = exoPlayer.duration
                val currentPosition = exoPlayer.currentPosition

                // We calculate the current point position relative to the length of the bar
                val thumbX = (currentPosition.toFloat() / duration) * width
                val touchX = event.x
                val distance = abs(thumbX - touchX)

                // Ignore touches that are far from the point
                if (distance > DRAG_THRESHOLD) return false
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * DO NOT CALL THIS METHOD DIRECTLY. Use [addSeekBarListener] instead!
     */
    override fun addListener(listener: OnScrubListener) {
        if (shouldAddListener) super.addListener(listener)
    }

    /**
     * Wrapper to circumvent adding the listener created by [PlayerControlView]
     */
    fun addSeekBarListener(listener: OnScrubListener) {
        shouldAddListener = true
        addListener(listener)
        shouldAddListener = false
    }

    fun setPlayer(player: Player) {
        this.exoPlayer = player
    }

    companion object {
        private const val MINIMUM_ACCEPTED_HEIGHT = -70f
        private const val DRAG_THRESHOLD = 40
    }
}