package com.github.libretube.ui.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.media3.session.MediaController
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.compat.PictureInPictureParamsCompat
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.helpers.PipHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.util.PlayingQueue

class PlayerPiPHelper(
    private val context: Context,
    private val handler: android.os.Handler,
    private val isPlayerInitialized: () -> Boolean,
    private val isPlaying: () -> Boolean,
    private val getNextVideo: () -> String?,
    private val onPlayNextVideo: (String) -> Unit,
    private val onSwitchToAudioMode: () -> Unit,
    private val onPipActivityReady: (Activity) -> Unit,
) {
    private var pipActivity: Activity? = null

    val playerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isPlayerInitialized()) return
            val event = intent.serializableExtra<PlayerEvent>(PlayerHelper.CONTROL_TYPE) ?: return

            // handle common player actions (play/pause, forward, rewind)
            val playerController = getPlayerController?.invoke() ?: return
            if (PlayerHelper.handlePlayerAction(playerController, event)) return

            when (event) {
                PlayerEvent.Next -> {
                    getNextVideo()?.let { onPlayNextVideo(it) }
                }
                PlayerEvent.Prev -> {
                    PlayingQueue.getPrev()?.let { onPlayNextVideo(it) }
                }
                PlayerEvent.Background -> {
                    onSwitchToAudioMode()
                    handler.postDelayed(500) {
                        pipActivity?.moveTaskToBack(false)
                        pipActivity = null
                    }
                }
                else -> Unit
            }
        }
    }

    var getPlayerController: (() -> MediaController)? = null
        private set

    fun registerReceiver(playerControllerProvider: () -> MediaController) {
        getPlayerController = playerControllerProvider
        ContextCompat.registerReceiver(
            context,
            playerActionReceiver,
            IntentFilter(PipHelper.getIntentActionName(context)),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterReceiver() {
        runCatching { context.unregisterReceiver(playerActionReceiver) }
        getPlayerController = null
    }

    val pipParams: PictureInPictureParamsCompat
        get() = run {
            val playing = isPlaying()

            PictureInPictureParamsCompat.Builder()
                .setActions(PlayerHelper.getPiPModeActions(context as Activity, playing))
                .setAutoEnterEnabled(playing)
                .apply {
                    if (playing) {
                        val controller = getPlayerController?.invoke()
                        if (controller != null) {
                            setAspectRatio(controller.videoSize)
                        }
                    }
                }
                .build()
        }

    fun isPipAvailable(): Boolean {
        return PictureInPictureCompat.isPictureInPictureAvailable(context)
    }

    fun shouldStartPiP(): Boolean {
        return isPipAvailable() && isPlayerInitialized() && isPlaying()
    }

    fun enterPipMode(activity: Activity) {
        PictureInPictureCompat.enterPictureInPictureMode(activity, pipParams)
    }

    fun updatePipParams(activity: Activity) {
        PictureInPictureCompat.setPictureInPictureParams(activity, pipParams)
    }

    fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        activity: Activity?,
        lifecycleState: androidx.lifecycle.Lifecycle.State,
        onEnterPiP: () -> Unit,
        onExitPiP: (wasClosed: Boolean) -> Unit,
    ) {
        if (isInPictureInPictureMode) {
            pipActivity = activity
            onEnterPiP()
        } else {
            val wasClosed = lifecycleState == androidx.lifecycle.Lifecycle.State.CREATED
            onExitPiP(wasClosed)
        }
    }

    fun setPipActivity(activity: Activity?) {
        pipActivity = activity
    }
}
