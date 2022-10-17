package com.github.libretube.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import coil.request.ImageRequest
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.ui.activities.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
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
                    putExtra(IntentData.videoId, videoId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
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
            var bitmap: Bitmap? = null
            var resizedBitmap: Bitmap? = null

            val request = ImageRequest.Builder(context)
                .data(streams?.thumbnailUrl)
                .target { result ->
                    bitmap = (result as BitmapDrawable).bitmap
                    resizedBitmap = Bitmap.createScaledBitmap(
                        bitmap!!,
                        bitmap!!.width,
                        bitmap!!.width,
                        false
                    )
                }
                .build()

            ImageHelper.imageLoader.enqueue(request)

            // returns the scaled bitmap if it got fetched successfully
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) resizedBitmap else bitmap
        }
    }

    /**
     * Creates a [MediaSessionCompat] amd a [MediaSessionConnector] for the player
     */
    private fun createMediaSession() {
        if (this::mediaSession.isInitialized) return
        mediaSession = MediaSessionCompat(context, this.javaClass.name)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
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
            setUseFastForwardActionInCompactView(true)
            setUseRewindActionInCompactView(true)
        }
    }

    /**
     * Destroy the [NowPlayingNotification]
     */
    fun destroy() {
        mediaSession.isActive = false
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
        playerNotification?.setPlayer(null)

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.cancel(PLAYER_NOTIFICATION_ID)

        player.stop()
        player.release()
    }
}
