package com.github.libretube.extensions

import androidx.media3.common.Player

fun Player.togglePlayPauseState() {
    when {
        playerError != null -> {
            prepare()
            play()
        }

       !isPlaying && playbackState == Player.STATE_ENDED -> {
            seekTo(0)
        }

        !isPlaying && !isLoading -> play()
        else -> pause()
    }
}