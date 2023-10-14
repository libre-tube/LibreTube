package com.github.libretube.extensions

import androidx.media3.common.Player
import com.github.libretube.helpers.PlayerHelper

fun Player.togglePlayPauseState() {
    val minBufferingReached = totalBufferedDuration >= PlayerHelper.MINIMUM_BUFFER_DURATION
            || currentPosition + PlayerHelper.MINIMUM_BUFFER_DURATION >= duration
    when {
        playerError != null -> {
            prepare()
            play()
        }

        !isPlaying && playbackState == Player.STATE_ENDED -> {
            seekTo(0)
        }

        !isPlaying && minBufferingReached -> play()
        else -> pause()
    }
}
