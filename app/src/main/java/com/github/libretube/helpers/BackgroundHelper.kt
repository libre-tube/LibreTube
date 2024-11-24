package com.github.libretube.helpers

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.github.libretube.constants.IntentData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.services.OnlinePlayerService
import com.github.libretube.services.VideoOfflinePlayerService
import com.github.libretube.services.VideoOnlinePlayerService
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.activities.NoInternetActivity
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.ui.fragments.PlayerFragment
import com.google.common.util.concurrent.MoreExecutors

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
            val fragmentManager =
                ContextHelper.unwrapActivity<MainActivity>(context).supportFragmentManager
            fragmentManager.fragments.firstOrNull { it is PlayerFragment }?.let {
                fragmentManager.commit { remove(it) }
            }
        }

        val playerData = PlayerData(videoId, playlistId, channelId, keepQueue, position)

        startMediaService(
            context,
            OnlinePlayerService::class.java,
            bundleOf(IntentData.playerData to playerData)
        )
    }

    /**
     * Stop the [OnlinePlayerService] service if it is running.
     */
    fun stopBackgroundPlay(context: Context) {
        arrayOf(
            OnlinePlayerService::class.java,
            OfflinePlayerService::class.java,
            VideoOfflinePlayerService::class.java,
            VideoOnlinePlayerService::class.java
        ).forEach {
            val intent = Intent(context, it)
            context.stopService(intent)
        }
    }

    /**
     * Check if the [OnlinePlayerService] service is currently running.
     */
    fun isBackgroundServiceRunning(
        context: Context,
        serviceClass: Class<*> = OnlinePlayerService::class.java
    ): Boolean {
        @Suppress("DEPRECATION")
        return context.getSystemService<ActivityManager>()!!.getRunningServices(Int.MAX_VALUE)
            .any { serviceClass.name == it.service.className }
    }

    /**
     * Start the offline background player
     *
     * @param context the current context
     * @param videoId the videoId of the video or null if all available downloads should be shuffled
     */
    fun playOnBackgroundOffline(
        context: Context,
        videoId: String?,
        downloadTab: DownloadTab,
        shuffle: Boolean = false
    ) {
        // whether the service is started from the MainActivity or NoInternetActivity
        val noInternet = ContextHelper.tryUnwrapActivity<NoInternetActivity>(context) != null

        val arguments = bundleOf(
            IntentData.videoId to videoId,
            IntentData.shuffle to shuffle,
            IntentData.downloadTab to downloadTab,
            IntentData.noInternet to noInternet
        )

        startMediaService(context, OfflinePlayerService::class.java, arguments)
    }

    @OptIn(UnstableApi::class)
    fun startMediaService(
        context: Context,
        serviceClass: Class<*>,
        arguments: Bundle = Bundle.EMPTY,
        sendStartCommand: Boolean = true,
        onController: (MediaController) -> Unit = {}
    ) {
        val sessionToken =
            SessionToken(context, ComponentName(context, serviceClass))

        val controllerFuture =
            MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            if (sendStartCommand) controller.sendCustomCommand(
                AbstractPlayerService.startServiceCommand,
                arguments
            )
            onController(controller)
        }, MoreExecutors.directExecutor())
    }
}
