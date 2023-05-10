package com.github.libretube.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import coil.request.ImageRequest
import com.github.libretube.R
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.ui.activities.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.CustomActionReceiver

class NowPlayingNotification(
    private val context: Context,
    private val player: ExoPlayer,
    private val isBackgroundPlayerNotification: Boolean,
) {
    private var videoId: String? = null
    private var notificationData: PlayerNotificationData? = null
    private var bitmap: Bitmap? = null

    /**
     * The [MediaSessionCompat] for the [notificationData].
     */
    private lateinit var mediaSession: MediaSessionCompat

    /**
     * The [MediaSessionConnector] to connect with the [mediaSession] and implement it with the [player].
     */
    private lateinit var mediaSessionConnector: MediaSessionConnector

    /**
     * The [PlayerNotificationManager] to load the [mediaSession] content on it.
     */
    private var playerNotification: PlayerNotificationManager? = null

    /**
     * The [descriptionAdapter] is used to show title, uploaderName and thumbnail of the video in the notification
     * Basic example [here](https://github.com/AnthonyMarkD/AudioPlayerSampleTest)
     */
    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
        /**
         * sets the title of the notification
         */
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return notificationData?.title.orEmpty()
        }

        /**
         * overrides the action when clicking the notification
         */
        override fun createCurrentContentIntent(player: Player): PendingIntent {
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

        /**
         * the description of the notification (below the title)
         */
        override fun getCurrentContentText(player: Player): CharSequence? {
            return notificationData?.uploaderName
        }

        /**
         * return the icon/thumbnail of the video
         */
        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {
            if (DataSaverMode.isEnabled(context)) return null

            if (bitmap == null) enqueueThumbnailRequest(callback)

            return bitmap
        }

        override fun getCurrentSubText(player: Player): CharSequence? {
            return notificationData?.uploaderName
        }
    }

    private fun enqueueThumbnailRequest(callback: PlayerNotificationManager.BitmapCallback) {
        // If playing a downloaded file, show the downloaded thumbnail instead of loading an
        // online image
        notificationData?.thumbnailPath?.let { path ->
            ImageHelper.getDownloadedImage(context, path)?.let {
                bitmap = processThumbnailBitmap(it)
                callback.onBitmap(bitmap!!)
            }
            return
        }

        val request = ImageRequest.Builder(context)
            .data(notificationData?.thumbnailUrl)
            .target {
                bitmap = processThumbnailBitmap(it.toBitmap())
                callback.onBitmap(bitmap!!)
            }
            .build()

        // enqueue the thumbnail loading request
        ImageHelper.imageLoader.enqueue(request)
    }

    private val customActionReceiver = object : CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int,
        ): MutableMap<String, NotificationCompat.Action> {
            return mutableMapOf(
                PREV to createNotificationAction(R.drawable.ic_prev_outlined, PREV, instanceId),
                NEXT to createNotificationAction(R.drawable.ic_next_outlined, NEXT, instanceId),
                REWIND to createNotificationAction(R.drawable.ic_rewind_md, REWIND, instanceId),
                FORWARD to createNotificationAction(R.drawable.ic_forward_md, FORWARD, instanceId),
            )
        }

        override fun getCustomActions(player: Player): MutableList<String> {
            return mutableListOf(PREV, NEXT, REWIND, FORWARD)
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            handlePlayerAction(action)
        }
    }

    /**
     *  Returns the bitmap on Android 13+, for everything below scaled down to a square
     */
    private fun processThumbnailBitmap(bitmap: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ImageHelper.getSquareBitmap(bitmap)
        } else {
            bitmap
        }
    }

    private fun createNotificationAction(drawableRes: Int, actionName: String, instanceId: Int): NotificationCompat.Action {
        val intent = Intent(actionName).setPackage(context.packageName)
        val pendingIntent = PendingIntentCompat
            .getBroadcast(context, instanceId, intent, PendingIntent.FLAG_CANCEL_CURRENT, false)
        return NotificationCompat.Action.Builder(drawableRes, actionName, pendingIntent).build()
    }

    private fun createMediaSessionAction(@DrawableRes drawableRes: Int, actionName: String): MediaSessionConnector.CustomActionProvider {
        return object : MediaSessionConnector.CustomActionProvider {
            override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                return PlaybackStateCompat.CustomAction.Builder(actionName, actionName, drawableRes).build()
            }

            override fun onCustomAction(player: Player, action: String, extras: Bundle?) {
                handlePlayerAction(action)
            }
        }
    }

    /**
     * Creates a [MediaSessionCompat] amd a [MediaSessionConnector] for the player
     */
    private fun createMediaSession() {
        if (this::mediaSession.isInitialized) return
        mediaSession = MediaSessionCompat(context, this.javaClass.name).apply {
            isActive = true
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlayer(player)
            setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                override fun getMediaDescription(
                    player: Player,
                    windowIndex: Int,
                ): MediaDescriptionCompat {
                    val appIcon = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.ic_launcher_monochrome,
                    )
                    val extras = bundleOf(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON to appIcon,
                        MediaMetadataCompat.METADATA_KEY_TITLE to notificationData?.title,
                        MediaMetadataCompat.METADATA_KEY_ARTIST to notificationData?.uploaderName,
                    )
                    return MediaDescriptionCompat.Builder()
                        .setTitle(notificationData?.title)
                        .setSubtitle(notificationData?.uploaderName)
                        .setIconBitmap(appIcon)
                        .setExtras(extras)
                        .build()
                }

                override fun getSupportedQueueNavigatorActions(player: Player): Long {
                    return PlaybackStateCompat.ACTION_PLAY_PAUSE
                }
            })
            setCustomActionProviders(
                createMediaSessionAction(R.drawable.ic_prev_outlined, PREV),
                createMediaSessionAction(R.drawable.ic_next_outlined, NEXT),
                createMediaSessionAction(R.drawable.ic_rewind_md, REWIND),
                createMediaSessionAction(R.drawable.ic_forward_md, FORWARD),
            )
        }
    }

    private fun handlePlayerAction(action: String) {
        when (action) {
            NEXT -> {
                if (PlayingQueue.hasNext()) {
                    PlayingQueue.onQueueItemSelected(
                        PlayingQueue.currentIndex() + 1,
                    )
                }
            }
            PREV -> {
                if (PlayingQueue.hasPrev()) {
                    PlayingQueue.onQueueItemSelected(
                        PlayingQueue.currentIndex() - 1,
                    )
                }
            }
            REWIND -> {
                player.seekTo(player.currentPosition - PlayerHelper.seekIncrement)
            }
            FORWARD -> {
                player.seekTo(player.currentPosition + PlayerHelper.seekIncrement)
            }
        }
    }

    /**
     * Updates or creates the [playerNotification]
     */
    fun updatePlayerNotification(
        videoId: String,
        data: PlayerNotificationData,
    ) {
        this.videoId = videoId
        this.notificationData = data
        // reset the thumbnail bitmap in order to become reloaded for the new video
        this.bitmap = null

        if (playerNotification == null) {
            createMediaSession()
            createNotification()
        }
    }

    /**
     * Initializes the [playerNotification] attached to the [player] and shows it.
     */
    private fun createNotification() {
        playerNotification = PlayerNotificationManager
            .Builder(context, PLAYER_NOTIFICATION_ID, BACKGROUND_CHANNEL_ID)
            // set the description of the notification
            .setMediaDescriptionAdapter(descriptionAdapter)
            // register the receiver for custom actions, doesn't seem to change anything
            .setCustomActionReceiver(customActionReceiver)
            .build().apply {
                setPlayer(player)
                setColorized(true)
                setMediaSessionToken(mediaSession.sessionToken)
                setSmallIcon(R.drawable.ic_launcher_lockscreen)
                setUseNextAction(false)
                setUsePreviousAction(false)
                setUseStopAction(true)
            }
    }

    /**
     * Destroy the [NowPlayingNotification]
     */
    fun destroySelfAndPlayer() {
        playerNotification?.setPlayer(null)

        mediaSession.isActive = false
        mediaSession.release()

        player.stop()
        player.release()

        context.getSystemService<NotificationManager>()!!.cancel(PLAYER_NOTIFICATION_ID)
    }

    companion object {
        private const val PREV = "prev"
        private const val NEXT = "next"
        private const val REWIND = "rewind"
        private const val FORWARD = "forward"
    }
}
