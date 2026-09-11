package com.github.libretube.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import androidx.media3.ui.TimeBar.OnScrubListener
import com.github.libretube.extensions.dpToPx
import kotlin.math.abs

@UnstableApi
open class DismissableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
): DefaultTimeBar(context, attributeSet) {
    var exoPlayer: Player? = null

    private val listeners = mutableListOf<OnScrubListener>()

    // Drag-only seeking state
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialPlaybackPosition: Long = 0
    private var currentScrubPosition: Long = 0
    private var shouldPlayerSeek: Boolean = true
    private var waitingForDrag: Boolean = false
    private var dragStarted: Boolean = false
    private val touchSlopPx: Int = ViewConfiguration.get(context).scaledTouchSlop

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val player = exoPlayer ?: return super.onTouchEvent(event)
        val duration = player.duration
        
        // Fallback to absolute seeking for live streams or invalid durations
        if (duration <= 0) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                waitingForDrag = true
                dragStarted = false
                // Consume to prevent tap-to-seek or thumb jump from DefaultTimeBar
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (waitingForDrag) {
                    val dx = abs(event.x - initialX)
                    val dy = abs(event.y - initialY)
                    if (dx > touchSlopPx || dy > touchSlopPx) {
                        waitingForDrag = false
                        dragStarted = true
                        initialPlaybackPosition = player.currentPosition
                        currentScrubPosition = initialPlaybackPosition
                        
                        // Notify listeners that scrubbing has started
                        listeners.forEach { it.onScrubStart(this, initialPlaybackPosition) }
                    }
                }

                if (dragStarted) {
                    val deltaX = event.x - initialX
                    val barWidth = width.toFloat()
                    
                    // Fine Seeking logic:
                    // As the finger moves vertically away from the bar, decrease seeking sensitivity.
                    val verticalDistance = abs(event.y - initialY)
                    val sensitivityLimit = 150f.dpToPx() // Maximum distance for scaling
                    val sensitivity = (1f - (verticalDistance / sensitivityLimit) * 0.8f).coerceIn(0.2f, 1f)
                    
                    // Calculate relative time offset
                    val deltaTime = (deltaX / barWidth) * duration * sensitivity
                    currentScrubPosition = (initialPlaybackPosition + deltaTime.toLong()).coerceIn(0, duration)
                    
                    // Update visual position of the bar
                    super.setPosition(currentScrubPosition)
                    
                    // Notify listeners (like SeekbarPreviewListener) to update preview
                    listeners.forEach { it.onScrubMove(this, currentScrubPosition) }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragStarted) {
                    val isCanceled = event.actionMasked == MotionEvent.ACTION_CANCEL
                    
                    // Determine if we should seek based on vertical drag limit (Dismissable behavior)
                    shouldPlayerSeek = !isCanceled &&
                            event.y > TOUCH_SEEK_LIMIT_ABOVE.dpToPx() &&
                            event.y < TOUCH_SEEK_LIMIT_BELOW.dpToPx()

                    // Notify listeners that scrubbing has stopped
                    listeners.forEach { it.onScrubStop(this, currentScrubPosition, isCanceled) }

                    if (shouldPlayerSeek) {
                        player.seekTo(currentScrubPosition)
                    }
                    
                    dragStarted = false
                    waitingForDrag = false
                    return true
                }

                waitingForDrag = false
                dragStarted = false
                performClick()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun setPosition(position: Long) {
        if (!dragStarted) {
            super.setPosition(position)
        }
    }

    /**
     * DO NOT CALL THIS METHOD DIRECTLY. Use [addSeekBarListener] instead!
     */
    @Deprecated("Use addSeekBarListener instead")
    override fun addListener(listener: OnScrubListener) {
        // Ignored
    }

    /**
     * DO NOT CALL THIS METHOD DIRECTLY. Use [removeSeekBarListener] instead!
     */
    @Deprecated("Use removeSeekBarListener instead")
    override fun removeListener(listener: OnScrubListener) {
        // Ignored
    }

    fun addSeekBarListener(listener: OnScrubListener) {
        listeners.add(listener)
    }

    fun removeSeekBarListener(listener: OnScrubListener) {
        listeners.remove(listener)
    }

    fun setPlayer(player: Player) {
        this.exoPlayer = player
    }

    companion object {
        private const val TOUCH_SEEK_LIMIT_ABOVE = -70f
        private const val TOUCH_SEEK_LIMIT_BELOW = 200f
    }
}
