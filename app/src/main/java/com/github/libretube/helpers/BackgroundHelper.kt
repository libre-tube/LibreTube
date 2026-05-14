package com.github.libretube.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.fragment.app.commit
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.github.libretube.constants.IntentData
import com.github.libretube.extensions.TAG
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.services.OnlinePlayerService
import com.github.libretube.ui.activities.AbstractPlayerHostActivity
import com.github.libretube.ui.activities.NoInternetActivity
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PlayingQueueMode
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors

/**
 * Helper for starting a new Instance of the [OnlinePlayerService]
 */
object BackgroundHelper {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Start the foreground service [OnlinePlayerService] to play in background.
     */
    fun playOnBackground(
        context: Context,
        playerData: PlayerData,
    ) {
        // close the previous video player if open
        val fragmentManager =
            ContextHelper.unwrapActivity<AbstractPlayerHostActivity>(context).supportFragmentManager
        fragmentManager.fragments.firstOrNull { it is PlayerFragment }?.let {
            fragmentManager.commit { remove(it) }
        }

        val noInternet = ContextHelper.tryUnwrapActivity<NoInternetActivity>(context) != null
        val service = if (playerData.isOffline) OfflinePlayerService::class.java else OnlinePlayerService::class.java
        stopBackgroundPlay(context)
        startMediaService(context, service, bundleOf(
            IntentData.playerData to playerData,
            IntentData.noInternet to noInternet,
            IntentData.audioOnly to true
        ))
    }

    /**
     * Stop the [OnlinePlayerService] or [OfflinePlayerService] service if it is running.
     */
    fun stopBackgroundPlay(context: Context) {
        arrayOf(
            OnlinePlayerService::class.java,
            OfflinePlayerService::class.java
        ).forEach {
            val intent = Intent(context, it)
            context.stopService(intent)
        }
    }

    @OptIn(UnstableApi::class)
    fun startMediaService(
        context: Context,
        serviceClass: Class<*>,
        arguments: Bundle = Bundle.EMPTY,
        onController: (MediaController) -> Unit = {}
    ) {
        val context = context.applicationContext
        val sessionToken =
            SessionToken(context, ComponentName(context, serviceClass))

        val controllerFuture =
            MediaController.Builder(context, sessionToken).buildAsync()
        Futures.addCallback(controllerFuture, object : FutureCallback<MediaController> {
            override fun onSuccess(result: MediaController?) {
                val controller = controllerFuture.get()
                if (!arguments.isEmpty) controller.sendCustomCommand(
                    AbstractPlayerService.startServiceCommand,
                    arguments
                )
                onController(controller)
            }

            // HACK:
            // if failing to connect to the media session, that's probably because
            // two media services were started at the same time
            // hence, we retry it here
            // see also: https://github.com/androidx/media/issues/1096
            override fun onFailure(t: Throwable) {
                Log.e(TAG(), t.toString())
                handler.postDelayed(200) {
                    startMediaService(context, serviceClass, arguments, onController)
                }
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Get the service class of the currently active player based on the playing queue mode
     */
    fun getCurrentPlayerServiceClass(): Class<*> {
        return if (PlayingQueue.queueMode == PlayingQueueMode.OFFLINE) {
            OfflinePlayerService::class.java
        } else {
            OnlinePlayerService::class.java
        }
    }

    /**
     * Start a media service for the currently active player (online or offline)
     * and pass the MediaController to the callback
     */
    fun startCurrentMediaService(
        context: Context,
        arguments: Bundle = Bundle.EMPTY,
        onController: (MediaController) -> Unit = {}
    ) {
        startMediaService(context, getCurrentPlayerServiceClass(), arguments, onController)
    }


    /**
     * Set the volume of the currently active player
     *
     * @param context the current context
     * @param volume the volume level to set (0.0 to 1.0)
     */
    fun setVolume(context: Context, volume: Float) {
        startCurrentMediaService(context, Bundle.EMPTY) { controller ->
            try {
                controller.volume = volume.coerceIn(0f, 1f)
            } finally {
                controller.release()
            }
        }
    }
}
