package com.github.libretube.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.request.ImageRequest
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_CHANNEL_ID
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.toMediaMetadataCompat
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.ui.activities.MainActivity

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NowPlayingNotification(
    private val context: Context,
    private val player: ExoPlayer,
    private val isBackgroundPlayerNotification: Boolean
) {
    private var videoId: String? = null
    private val nManager = context.getSystemService<NotificationManager>()!!

    /**
     * The metadata of the current playing song (thumbnail, title, uploader)
     */
    private var notificationData: PlayerNotificationData? = null

    /**
     * The [MediaSessionCompat] for the [notificationData].
     */
    private lateinit var mediaSession: MediaSessionCompat

    /**
     * The [NotificationCompat.Builder] to load the [mediaSession] content on it.
     */
    private var notificationBuilder: NotificationCompat.Builder? = null

    /**
     * The [Bitmap] which represents the background / thumbnail of the notification
     */
    private var notificationBitmap: Bitmap? = null

    private fun loadCurrentLargeIcon() {
        if (DataSaverMode.isEnabled(context)) return
        if (notificationBitmap == null) {
            enqueueThumbnailRequest {
                createOrUpdateNotification()
            }
        }
    }

    private fun createCurrentContentIntent(): PendingIntent? {
        // starts a new MainActivity Intent when the player notification is clicked
        // it doesn't start a completely new MainActivity because the MainActivity's launchMode
        // is set to "singleTop" in the AndroidManifest (important!!!)
        // that's the only way to launch back into the previous activity (e.g. the player view
        val intent = Intent(context, MainActivity::class.java).apply {
            if (isBackgroundPlayerNotification) {
                putExtra(IntentData.openAudioPlayer, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        return PendingIntentCompat
            .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)
    }

    private fun createIntent(action: String): PendingIntent? {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntentCompat
            .getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT, false)
    }

    private fun enqueueThumbnailRequest(callback: (Bitmap) -> Unit) {
        // If playing a downloaded file, show the downloaded thumbnail instead of loading an
        // online image
        notificationData?.thumbnailPath?.let { path ->
            ImageHelper.getDownloadedImage(context, path)?.let {
                notificationBitmap = processBitmap(it)
                callback.invoke(notificationBitmap!!)
            }
            return
        }

        val request = ImageRequest.Builder(context)
            .data(notificationData?.thumbnailUrl)
            .target {
                notificationBitmap = processBitmap(it.toBitmap())
                callback.invoke(notificationBitmap!!)
            }
            .build()

        // enqueue the thumbnail loading request
        ImageHelper.imageLoader.enqueue(request)
    }

    private fun processBitmap(bitmap: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bitmap
        } else {
            ImageHelper.getSquareBitmap(bitmap)
        }
    }

    private val legacyNotificationButtons
        get() = listOf(
            createNotificationAction(R.drawable.ic_prev_outlined, PREV),
            createNotificationAction(
                if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                PLAY_PAUSE
            ),
            createNotificationAction(R.drawable.ic_next_outlined, NEXT),
            createNotificationAction(R.drawable.ic_rewind_md, REWIND),
            createNotificationAction(R.drawable.ic_forward_md, FORWARD)
        )

    private fun createNotificationAction(
        drawableRes: Int,
        actionName: String
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(drawableRes, actionName, createIntent(actionName))
            .build()
    }

    private fun createMediaSessionAction(
        @DrawableRes drawableRes: Int,
        actionName: String
    ): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(actionName, actionName, drawableRes).build()
    }

    /**
     * Creates a [MediaSessionCompat] for the player
     */
    private fun createMediaSession() {
        if (this::mediaSession.isInitialized) return

        val sessionCallback = object : MediaSessionCompat.Callback() {
            override fun onSkipToNext() {
                handlePlayerAction(NEXT)
                super.onSkipToNext()
            }

            override fun onSkipToPrevious() {
                handlePlayerAction(PREV)
                super.onSkipToPrevious()
            }

            override fun onRewind() {
                handlePlayerAction(REWIND)
                super.onRewind()
            }

            override fun onFastForward() {
                handlePlayerAction(FORWARD)
                super.onFastForward()
            }

            override fun onPlay() {
                handlePlayerAction(PLAY_PAUSE)
                super.onPlay()
            }

            override fun onPause() {
                handlePlayerAction(PLAY_PAUSE)
                super.onPause()
            }

            override fun onStop() {
                handlePlayerAction(STOP)
                super.onStop()
            }

            override fun onSeekTo(pos: Long) {
                player.seekTo(pos)
                super.onSeekTo(pos)
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                handlePlayerAction(action)
                super.onCustomAction(action, extras)
            }
        }

        mediaSession = MediaSessionCompat(context, TAG())
        mediaSession.setCallback(sessionCallback)

        updateSessionMetadata()
        updateSessionPlaybackState()

        val playerStateListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                updateSessionPlaybackState(isPlaying = isPlaying)
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)

                if (!isLoading) {
                    updateSessionMetadata()
                }

                updateSessionPlaybackState(isLoading = isLoading)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                updateSessionMetadata(mediaMetadata)
            }
        }

        player.addListener(playerStateListener)
    }

    private fun updateSessionMetadata(metadata: MediaMetadata? = null) {
        val data = metadata ?: player.mediaMetadata
        val newMetadata = data.toMediaMetadataCompat(player.duration, notificationBitmap)
        mediaSession.setMetadata(newMetadata)
    }

    private fun updateSessionPlaybackState(isPlaying: Boolean? = null, isLoading: Boolean? = null) {
        val loading = isLoading == true || (isPlaying == false && player.isLoading)

        val newPlaybackState = if (loading) {
            createPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        } else if (isPlaying ?: player.isPlaying) {
            createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        } else {
            createPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        mediaSession.setPlaybackState(newPlaybackState)
    }

    private fun createPlaybackState(@PlaybackStateCompat.State state: Int): PlaybackStateCompat {
        val stateActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_REWIND or
            PlaybackStateCompat.ACTION_FAST_FORWARD or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SEEK_TO

        return PlaybackStateCompat.Builder()
            .setActions(stateActions)
            .addCustomAction(createMediaSessionAction(R.drawable.ic_rewind_md, REWIND))
            .addCustomAction(createMediaSessionAction(R.drawable.ic_forward_md, FORWARD))
            .setState(state, player.currentPosition, player.playbackParameters.speed)
            .build()
    }

    private fun handlePlayerAction(action: String) {
        when (action) {
            NEXT -> {
                if (PlayingQueue.hasNext()) {
                    PlayingQueue.onQueueItemSelected(
                        PlayingQueue.currentIndex() + 1
                    )
                }
            }

            PREV -> {
                if (PlayingQueue.hasPrev()) {
                    PlayingQueue.onQueueItemSelected(
                        PlayingQueue.currentIndex() - 1
                    )
                }
            }

            REWIND -> {
                player.seekBy(-PlayerHelper.seekIncrement)
            }

            FORWARD -> {
                player.seekBy(PlayerHelper.seekIncrement)
            }

            PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }

            STOP -> {
                Log.e("stop", "stop")
                if (isBackgroundPlayerNotification) {
                    BackgroundHelper.stopBackgroundPlay(context)
                }
            }
        }
    }

    /**
     * Updates or creates the [notificationBuilder]
     */
    fun updatePlayerNotification(
        videoId: String,
        data: PlayerNotificationData
    ) {
        this.videoId = videoId
        this.notificationData = data
        // reset the thumbnail bitmap in order to become reloaded for the new video
        this.notificationBitmap = null

        loadCurrentLargeIcon()

        if (notificationBuilder == null) {
            createMediaSession()
            createNotificationBuilder()
            createActionReceiver()
            // update the notification each time the player continues playing or pauses
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    createOrUpdateNotification()
                    super.onIsPlayingChanged(isPlaying)
                }
            })
        }

        createOrUpdateNotification()
    }

    /**
     * Initializes the [notificationBuilder] attached to the [player] and shows it.
     */
    private fun createNotificationBuilder() {
        notificationBuilder = NotificationCompat.Builder(context, PLAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setContentIntent(createCurrentContentIntent())
            .setDeleteIntent(createIntent(STOP))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1)
            )
    }

    private fun createOrUpdateNotification() {
        if (notificationBuilder == null) return
        val notification = notificationBuilder!!
            .setContentTitle(notificationData?.title)
            .setContentText(notificationData?.uploaderName)
            .setLargeIcon(notificationBitmap)
            .clearActions()
            .apply {
                legacyNotificationButtons.forEach {
                    addAction(it)
                }
            }
            .build()
        updateSessionMetadata()
        nManager.notify(PLAYER_NOTIFICATION_ID, notification)
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handlePlayerAction(intent.action ?: return)
        }
    }

    private fun createActionReceiver() {
        listOf(PREV, NEXT, REWIND, FORWARD, PLAY_PAUSE, STOP).forEach {
            context.registerReceiver(notificationActionReceiver, IntentFilter(it))
        }
    }

    /**
     * Destroy the [NowPlayingNotification]
     */
    fun destroySelfAndPlayer() {
        mediaSession.release()

        player.stop()
        player.release()

        runCatching {
            context.unregisterReceiver(notificationActionReceiver)
        }

        nManager.cancel(PLAYER_NOTIFICATION_ID)
    }

    companion object {
        private const val PREV = "prev"
        private const val NEXT = "next"
        private const val REWIND = "rewind"
        private const val FORWARD = "forward"
        private const val PLAY_PAUSE = "play_pause"
        private const val STOP = "stop"
    }
}
