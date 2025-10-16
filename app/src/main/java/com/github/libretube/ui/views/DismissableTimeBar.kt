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
import kotlin.math.absoluteValue

@UnstableApi
open class DismissableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
): DefaultTimeBar(context, attributeSet) {
    var exoPlayer: Player? = null
    private var lastYPosition = 0f
    private var lastXPosition = 0f

    private val listeners = mutableListOf<OnScrubListener>()

    init {
        super.addListener(object : OnScrubListener {
            private var started: Boolean = false

            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                // only trigger scrub start if it's close to the current player position
                // otherwise dragging is required in order to seek forward
                exoPlayer?.let { exoPlayer ->
                    val widthWithoutPadding = width - (paddingStart + paddingEnd)
                    val touchProgressPercentage = lastXPosition / widthWithoutPadding
                    val currentPlayerProgressPercentage = exoPlayer.currentPosition.toFloat() / exoPlayer.duration

                    if ((touchProgressPercentage - currentPlayerProgressPercentage).absoluteValue > DRAG_THRESHOLD_PERCENT) {
                        return
                    }
                }

                started = true
                listeners.forEach { it.onScrubStart(timeBar, position) }
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                if (!started) {
                    listeners.forEach { it.onScrubStart(timeBar, position) }
                    started = true
                }

                listeners.forEach { it.onScrubMove(timeBar, position) }
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                if (!started) return

                listeners.forEach { it.onScrubStop(timeBar, position, canceled) }
                started = false

                val exoPlayer = exoPlayer ?: return
                if (lastYPosition <= MINIMUM_ACCEPTED_HEIGHT.dpToPx()) return
                if (!canceled) exoPlayer.seekTo(position)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastYPosition = event.y
        lastXPosition = event.x

        return super.onTouchEvent(event)
    }

    @Deprecated("DO NOT CALL THIS METHOD DIRECTLY. Use [addSeekBarListener] instead!")
    override fun addListener(listener: OnScrubListener) {
        // do nothing - listeners should only be set via addSeekBarListener
    }

    /**
     * Wrapper to circumvent adding the listener created by [PlayerControlView]
     */
    fun addSeekBarListener(listener: OnScrubListener) {
        listeners.add(listener)
    }

    fun setPlayer(player: Player) {
        this.exoPlayer = player
    }

    companion object {
        private const val MINIMUM_ACCEPTED_HEIGHT = -70f
        private const val DRAG_THRESHOLD_PERCENT = 0.05
    }
}