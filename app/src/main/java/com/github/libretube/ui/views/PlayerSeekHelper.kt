package com.github.libretube.ui.views

import android.os.Handler
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.github.libretube.databinding.DoubleTapOverlayBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.extensions.navigateVideo
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.views.CustomExoPlayerView.Companion.ANIMATION_DURATION
import com.github.libretube.util.PlayingQueue

class PlayerSeekHelper(
    private val playerProvider: () -> Player?,
    private val gestureBinding: () -> DoubleTapOverlayBinding,
    private val controlBinding: () -> ExoStyledPlayerControlViewBinding,
    private val fastForwardViewProvider: () -> android.view.View,
    private val isPlayerLocked: () -> Boolean,
    private val handler: Handler,
    private val hideForwardToken: String,
    private val hideRewindToken: String,
) {
    private var rememberedPlaybackSpeed: Float? = null

    fun initRewindAndForward() {
        val seekIncrementText = (PlayerHelper.seekIncrement / 1000).toString()
        val doubleTapBinding = gestureBinding()
        val ctrlBinding = controlBinding()

        listOf(
            doubleTapBinding.rewindLayout.rewindTV,
            doubleTapBinding.forwardLayout.forwardTV,
            ctrlBinding.seekButtonForward.forwardTV,
            ctrlBinding.seekButtonRewind.rewindTV
        ).forEach { it.text = seekIncrementText }

        ctrlBinding.seekButtonForward.forwardBTN.setOnClickListener {
            playerProvider()?.seekBy(PlayerHelper.seekIncrement)
        }
        ctrlBinding.seekButtonRewind.rewindBTN.setOnClickListener {
            playerProvider()?.seekBy(-PlayerHelper.seekIncrement)
        }

        if (!PlayerHelper.doubleTapToSeek) {
            ctrlBinding.seekButtonForward.forwardBTN.isVisible = !isPlayerLocked()
            ctrlBinding.seekButtonRewind.rewindBTN.isVisible = !isPlayerLocked()
        }
    }

    fun rewind() {
        playerProvider()?.seekBy(-PlayerHelper.seekIncrement)

        gestureBinding().apply {
            animateSeeking(
                rewindLayout.rewindBTN,
                rewindLayout.rewindIV,
                rewindLayout.rewindTV,
                isRewind = true
            )

            handler.removeCallbacksAndMessages(hideRewindToken)
            handler.postDelayed(700, hideRewindToken) {
                rewindLayout.rewindBTN.isGone = true
            }
        }
    }

    fun forward() {
        playerProvider()?.seekBy(PlayerHelper.seekIncrement)

        gestureBinding().apply {
            animateSeeking(
                forwardLayout.forwardBTN,
                forwardLayout.forwardIV,
                forwardLayout.forwardTV,
                isRewind = false
            )

            handler.removeCallbacksAndMessages(hideForwardToken)
            handler.postDelayed(700, hideForwardToken) {
                forwardLayout.forwardBTN.isGone = true
            }
        }
    }

    fun onLongPress() {
        if (!PlayerHelper.longPressFastForward) return

        fastForwardViewProvider().isVisible = true
        val player = playerProvider() ?: return

        if (player.playbackParameters.speed >= PlayerHelper.MAXIMUM_PLAYBACK_SPEED) return

        rememberedPlaybackSpeed = player.playbackParameters.speed
        val newSpeed = minOf(
            player.playbackParameters.speed * PlayerHelper.FAST_FORWARD_SPEED_FACTOR,
            PlayerHelper.MAXIMUM_PLAYBACK_SPEED
        )
        player.playbackParameters =
            androidx.media3.common.PlaybackParameters(newSpeed, player.playbackParameters.pitch)
    }

    fun onLongPressEnd() {
        if (!PlayerHelper.longPressFastForward) return

        fastForwardViewProvider().isGone = true

        val player = playerProvider() ?: return
        rememberedPlaybackSpeed?.let {
            player.playbackParameters =
                androidx.media3.common.PlaybackParameters(it, player.playbackParameters.pitch)
        }
        rememberedPlaybackSpeed = null
    }

    fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        val player = playerProvider() ?: return false
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_SPACE, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player.togglePlayPauseState()
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                forward()
            }
            android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                rewind()
            }
            android.view.KeyEvent.KEYCODE_N, android.view.KeyEvent.KEYCODE_NAVIGATE_NEXT -> {
                PlayingQueue.getNext()?.let { (player as? MediaController)?.navigateVideo(it) }
            }
            android.view.KeyEvent.KEYCODE_P, android.view.KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                PlayingQueue.getPrev()?.let { (player as? MediaController)?.navigateVideo(it) }
            }
            else -> return false
        }
        return true
    }

    private fun animateSeeking(
        container: FrameLayout,
        imageView: ImageView,
        textView: TextView,
        isRewind: Boolean
    ) {
        container.isVisible = true
        val direction = if (isRewind) -1 else 1

        imageView.animate().rotation(0F).setDuration(0).start()
        textView.animate().translationX(0f).setDuration(0).start()

        imageView.animate()
            .rotation(direction * 30F)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                imageView.animate().rotation(0F).setDuration(ANIMATION_DURATION).start()
            }
            .start()

        textView.animate()
            .translationX(direction * 100f)
            .setDuration((ANIMATION_DURATION * 1.5).toLong())
            .withEndAction {
                handler.postDelayed(100) {
                    textView.animate().setDuration(ANIMATION_DURATION / 2).translationX(0f).start()
                }
            }
            .start()
    }
}
