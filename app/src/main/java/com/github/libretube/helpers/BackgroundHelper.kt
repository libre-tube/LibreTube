package com.github.libretube.helpers

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import com.github.libretube.constants.IntentData
import com.github.libretube.services.BackgroundMode
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.fragments.PlayerFragment

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
        keepQueue: Boolean? = null,
        keepVideoPlayerAlive: Boolean = false
    ) {
        // close the previous video player if open
        if (!keepVideoPlayerAlive) {
            (context as? MainActivity)?.supportFragmentManager?.let { fragmentManager ->
                fragmentManager.fragments.firstOrNull { it is PlayerFragment }?.let {
                    fragmentManager.commit { remove(it) }
                }
            }
        }

        // create an intent for the background mode service
        val intent = Intent(context, BackgroundMode::class.java)
        intent.putExtra(IntentData.videoId, videoId)
        intent.putExtra(IntentData.playlistId, playlistId)
        intent.putExtra(IntentData.channelId, channelId)
        intent.putExtra(IntentData.position, position)
        intent.putExtra(IntentData.keepQueue, keepQueue)

        // start the background mode as foreground service
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stop the [BackgroundMode] service if it is running.
     */
    fun stopBackgroundPlay(context: Context) {
        if (isBackgroundServiceRunning(context)) {
            // Intent to stop background mode service
            val intent = Intent(context, BackgroundMode::class.java)
            context.stopService(intent)
        }
    }

    /**
     * Check if the [BackgroundMode] service is currently running.
     */
    fun isBackgroundServiceRunning(context: Context): Boolean {
        @Suppress("DEPRECATION")
        return context.getSystemService<ActivityManager>()!!.getRunningServices(Int.MAX_VALUE)
            .any { BackgroundMode::class.java.name == it.service.className }
    }
}
