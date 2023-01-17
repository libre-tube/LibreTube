package com.github.libretube.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import coil.request.ImageRequest
import com.github.libretube.R
import com.github.libretube.api.obj.Streams
import com.github.libretube.compat.PendingIntentCompat
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
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
    private val isBackgroundPlayerNotification: Boolean
) {
    private var videoId: String? = null
    private var streams: Streams? = null

    /**
     * The [MediaSessionCompat] for the [streams].
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
            return streams?.title!!
        }

        /**
         * overrides the action when clicking the notification
         */
        @SuppressLint("UnspecifiedImmutableFlag")
        override fun createCurrentContentIntent(player: Player): PendingIntent? {
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
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntentCompat.updateCurrentFlags
            )
        }

        /**
         * the description of the notification (below the title)
         */
        override fun getCurrentContentText(player: Player): CharSequence? {
            return streams?.uploader
        }

        /**
         * return the icon/thumbnail of the video
         */
        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            if (DataSaverMode.isEnabled(context)) return null

            var bitmap: Bitmap? = null

            val request = ImageRequest.Builder(context)
                .data(streams?.thumbnailUrl)
                .target { result ->
                    val bm = (result as BitmapDrawable).bitmap
                    // returns the bitmap on Android 13+, for everything below scaled down to a square
                    bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        ImageHelper.getSquareBitmap(bm)
                    } else {
                        bm
                    }
                    callback.onBitmap(bitmap!!)
                }
                .build()

            // enqueue the thumbnail loading request
            ImageHelper.imageLoader.enqueue(request)

            return bitmap
        }

        override fun getCurrentSubText(player: Player): CharSequence? {
            return streams?.uploader
        }
    }

    private val customActionReceiver = object : CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): MutableMap<String, NotificationCompat.Action> {
            return mutableMapOf(
                PREV to createNotificationAction(R.drawable.ic_prev_outlined, PREV, instanceId),
                NEXT to createNotificationAction(R.drawable.ic_next_outlined, NEXT, instanceId),
                REWIND to createNotificationAction(R.drawable.ic_rewind_md, REWIND, instanceId),
                FORWARD to createNotificationAction(R.drawable.ic_forward_md, FORWARD, instanceId)
            )
        }

        override fun getCustomActions(player: Player): MutableList<String> {
            return mutableListOf(PREV, NEXT, REWIND, FORWARD)
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            handlePlayerAction(action)
        }
    }

    private fun createNotificationAction(drawableRes: Int, actionName: String, instanceId: Int): NotificationCompat.Action {
        val intent: Intent = Intent(actionName).setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            instanceId,
            intent,
            PendingIntentCompat.cancelCurrentFlags
        )
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
                    windowIndex: Int
                ): MediaDescriptionCompat {
                    return MediaDescriptionCompat.Builder().apply {
                        setTitle(streams?.title!!)
                        setSubtitle(streams?.uploader)
                        val appIcon = BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.ic_launcher_monochrome
                        )
                        val extras = Bundle().apply {
                            putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, appIcon)
                            putString(MediaMetadataCompat.METADATA_KEY_TITLE, streams?.title!!)
                            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, streams?.uploader)
                        }
                        setIconBitmap(appIcon)
                        setExtras(extras)
                    }.build()
                }

                override fun getSupportedQueueNavigatorActions(player: Player): Long {
                    return PlaybackStateCompat.ACTION_PLAY_PAUSE
                }
            })
            setCustomActionProviders(
                createMediaSessionAction(R.drawable.ic_prev_outlined, PREV),
                createMediaSessionAction(R.drawable.ic_next_outlined, NEXT),
                createMediaSessionAction(R.drawable.ic_rewind_md, REWIND),
                createMediaSessionAction(R.drawable.ic_forward_md, FORWARD)
            )
        }
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
        streams: Streams
    ) {
        this.videoId = videoId
        this.streams = streams

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

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.cancel(PLAYER_NOTIFICATION_ID)
    }

    companion object {
        private const val PREV = "prev"
        private const val NEXT = "next"
        private const val REWIND = "rewind"
        private const val FORWARD = "forward"
    }
}
