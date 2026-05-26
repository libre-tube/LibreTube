package com.github.libretube.extensions

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.services.AbstractPlayerService

@OptIn(UnstableApi::class)
fun MediaController.navigateVideo(videoId: String) {
    sendCustomCommand(
        AbstractPlayerService.runPlayerActionCommand,
        Bundle().apply {
            putString(PlayerCommand.PLAY_VIDEO_BY_ID.name, videoId)
        },
    )
}
