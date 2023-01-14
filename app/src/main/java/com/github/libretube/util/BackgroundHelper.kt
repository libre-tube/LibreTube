package com.github.libretube.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.github.libretube.compat.ServiceCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.services.BackgroundMode

/**
 * Helper for starting a new Instance of the [BackgroundMode]
 */
object BackgroundHelper {

    /**
     * Start the foreground service [BackgroundMode] to play in background. [position]
     * is seek to position specified in milliseconds in the current [videoId].
     */
    fun playOnBackground(
        context: Context,
        videoId: String,
        position: Long? = null,
        playlistId: String? = null,
        channelId: String? = null,
        keepQueue: Boolean? = null
    ) {
        // create an intent for the background mode service
        val intent = Intent(context, BackgroundMode::class.java)
        intent.putExtra(IntentData.videoId, videoId)
        intent.putExtra(IntentData.playlistId, playlistId)
        intent.putExtra(IntentData.channelId, channelId)
        intent.putExtra(IntentData.position, position)
        intent.putExtra(IntentData.keepQueue, keepQueue)

        // start the background mode as foreground service
        ServiceCompat(context).startForeground(intent)
    }

    /**
     * Stop the [BackgroundMode] service if it is running.
     */
    fun stopBackgroundPlay(context: Context) {
        if (!isServiceRunning(context, BackgroundMode::class.java)) return

        // Intent to stop background mode service
        val intent = Intent(context, BackgroundMode::class.java)
        context.stopService(intent)
    }

    /**
     * Check if the given service as [serviceClass] is currently running.
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
