package com.github.libretube.extensions

import androidx.media3.common.Player
import com.github.libretube.helpers.PlayerHelper

fun Player.togglePlayPauseState() {
    when {
        playerError != null -> {
            prepare()
            play()
        }

        !isPlaying && playbackState == Player.STATE_ENDED -> {
            seekTo(0)
        }

        !isPlaying && totalBufferedDuration > PlayerHelper.MINIMUM_BUFFER_DURATION -> play()
        else -> pause()
    }
}
