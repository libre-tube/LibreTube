package com.github.libretube.helpers

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import com.github.libretube.constants.IntentData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.services.OnlinePlayerService
import com.github.libretube.ui.fragments.PlayerFragment

/**
 * Helper for starting a new Instance of the [OnlinePlayerService]
 */
object BackgroundHelper {

    /**
     * Start the foreground service [OnlinePlayerService] to play in background. [position]
     * is seek to position specified in milliseconds in the current [videoId].
     */
    fun playOnBackground(
        context: Context,
        videoId: String,
        position: Long = 0,
        playlistId: String? = null,
        channelId: String? = null,
        keepQueue: Boolean = false,
        keepVideoPlayerAlive: Boolean = false
    ) {
        // close the previous video player if open
        if (!keepVideoPlayerAlive) {
            val fragmentManager = ContextHelper.unwrapActivity(context).supportFragmentManager
            fragmentManager.fragments.firstOrNull { it is PlayerFragment }?.let {
                fragmentManager.commit { remove(it) }
            }
        }

        // create an intent for the background mode service
        val playerData = PlayerData(videoId, playlistId, channelId, keepQueue, position)
        val intent = Intent(context, OnlinePlayerService::class.java)
            .putExtra(IntentData.playerData, playerData)

        // start the background mode as foreground service
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stop the [OnlinePlayerService] service if it is running.
     */
    fun stopBackgroundPlay(context: Context) {
        if (isBackgroundServiceRunning(context)) {
            // Intent to stop background mode service
            val intent = Intent(context, OnlinePlayerService::class.java)
            context.stopService(intent)
        }
    }

    /**
     * Check if the [OnlinePlayerService] service is currently running.
     */
    fun isBackgroundServiceRunning(context: Context): Boolean {
        @Suppress("DEPRECATION")
        return context.getSystemService<ActivityManager>()!!.getRunningServices(Int.MAX_VALUE)
            .any { OnlinePlayerService::class.java.name == it.service.className }
    }
}
