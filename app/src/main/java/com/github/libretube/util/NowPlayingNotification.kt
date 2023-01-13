package com.github.libretube.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import coil.request.ImageRequest
import com.github.libretube.R
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.ui.activities.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager

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
     * The [DescriptionAdapter] is used to show title, uploaderName and thumbnail of the video in the notification
     * Basic example [here](https://github.com/AnthonyMarkD/AudioPlayerSampleTest)
     */
    inner class DescriptionAdapter :
        PlayerNotificationManager.MediaDescriptionAdapter {
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
            //  that's the only way to launch back into the previous activity (e.g. the player view
            val intent = Intent(context, MainActivity::class.java).apply {
                if (isBackgroundPlayerNotification) {
                    putExtra(IntentData.openAudioPlayer, true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
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
                    bitmap = (result as BitmapDrawable).bitmap
                }
                .build()

            ImageHelper.imageLoader.enqueue(request)

            // returns the bitmap on Android 13+, for everything below scaled down to a square
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) getSquareBitmap(bitmap) else bitmap
        }
    }

    private fun getSquareBitmap(bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null
        val newSize = minOf(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(
            bitmap,
            (bitmap.width - newSize) / 2,
            (bitmap.height - newSize) / 2,
            newSize,
            newSize
        )
    }

    /**
     * Creates a [MediaSessionCompat] amd a [MediaSessionConnector] for the player
     */
    private fun createMediaSession() {
        if (this::mediaSession.isInitialized) return
        mediaSession = MediaSessionCompat(context, this.javaClass.name)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return MediaDescriptionCompat.Builder().apply {
                    setTitle(streams?.title!!)
                    setSubtitle(streams?.uploader)
                    val extras = Bundle()
                    val appIcon = BitmapFactory.decodeResource(
                        Resources.getSystem(),
                        R.drawable.ic_launcher_monochrome
                    )
                    extras.putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, appIcon)
                    extras.putString(MediaMetadataCompat.METADATA_KEY_TITLE, streams?.title!!)
                    extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, streams?.uploader)
                    setIconBitmap(appIcon)
                    setExtras(extras)
                }.build()
            }
        })
        mediaSessionConnector.setPlayer(player)
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
            .setMediaDescriptionAdapter(
                DescriptionAdapter()
            )
            .build()
        playerNotification?.apply {
            setPlayer(player)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setUseStopAction(true)
            setColorized(true)
            setMediaSessionToken(mediaSession.sessionToken)
            setSmallIcon(R.drawable.ic_launcher_lockscreen)
            setUseFastForwardActionInCompactView(true)
            setUseRewindActionInCompactView(true)
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
}
