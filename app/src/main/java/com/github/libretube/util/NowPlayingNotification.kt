package com.github.libretube.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaConstants
import coil.request.ImageRequest
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_CHANNEL_ID
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.seekBy
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

    private fun createCurrentContentIntent(): PendingIntent {
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
        return PendingIntentCompat.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT, false)
    }

    private fun createDeleteIntent(): PendingIntent {
        val intent = Intent(STOP).setPackage(context.packageName)
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
        val intent = Intent(actionName).setPackage(context.packageName)
        val pendingIntent = PendingIntentCompat
            .getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT, false)
        return NotificationCompat.Action.Builder(drawableRes, actionName, pendingIntent).build()
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

        val playbackState = if (player.isPlaying) {
            createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        } else {
            createPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        mediaSession = MediaSessionCompat(context, TAG())
        mediaSession.setCallback(sessionCallback)
        mediaSession.setPlaybackState(playbackState)
        mediaSession.setMetadata(getMetadataFromPlayer(player.mediaMetadata))

        val playerStateListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                val newPlaybackState = if (isPlaying) {
                    createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    createPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }

                mediaSession.setPlaybackState(newPlaybackState)
                mediaSession.setMetadata(getMetadataFromPlayer(player.mediaMetadata))
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)

                if (!isLoading) {
                    mediaSession.setMetadata(getMetadataFromPlayer(player.mediaMetadata))
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                mediaSession.setMetadata(getMetadataFromPlayer(mediaMetadata))
            }
        }

        player.addListener(playerStateListener)
    }

    private fun getMetadataFromPlayer(metadata: MediaMetadata): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()

        metadata.title?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, it)
            builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, it)
        }

        metadata.subtitle?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, it)
        }

        metadata.description?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, it)
        }

        metadata.artist?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
        }

        metadata.albumTitle?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
        }

        metadata.albumArtist?.let {
            builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, it)
        }

        metadata.recordingYear?.toLong()?.let {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, it)
        }

        metadata.artworkUri?.toString()?.let {
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it)
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it)
        }

        val playerDuration = player.duration

        if (playerDuration != C.TIME_UNSET) {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playerDuration)
        }

        metadata.mediaType?.toLong()?.let {
            builder.putLong(MediaConstants.EXTRAS_KEY_MEDIA_TYPE_COMPAT, it)
        }

        return builder.build()
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
            .setDeleteIntent(createDeleteIntent())
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
