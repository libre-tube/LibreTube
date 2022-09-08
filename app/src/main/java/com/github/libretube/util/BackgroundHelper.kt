package com.github.libretube.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.libretube.constants.IntentData
import com.github.libretube.services.BackgroundMode

/**
 * Helper for starting a new Instance of the [BackgroundMode]
 */
object BackgroundHelper {
    fun playOnBackground(
        context: Context,
        videoId: String,
        position: Long? = null,
        playlistId: String? = null
    ) {
        // create an intent for the background mode service
        val intent = Intent(context, BackgroundMode::class.java)
        intent.putExtra(IntentData.videoId, videoId)
        if (playlistId != null) intent.putExtra(IntentData.playlistId, playlistId)
        if (position != null) intent.putExtra("position", position)

        // start the background mode as foreground service
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
