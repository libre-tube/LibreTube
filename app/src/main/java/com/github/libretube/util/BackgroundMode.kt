package com.github.libretube.util

import android.app.NotificationManager
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import com.github.libretube.obj.Streams
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Loads the selected video audio in background mode with a notification area.
 */
class BackgroundMode {
    /**
     * The response that gets when called the Api.
     */
    private var response: Streams? = null

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    private var player: ExoPlayer? = null
    private var playWhenReadyPlayer = true

    /**
     * The [MediaSessionCompat] for the [response].
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
     * The [AudioAttributes] handle the audio focus of the [player]
     */
    private lateinit var audioAttributes: AudioAttributes

    /**
     * Initializes the [player] with the [MediaItem].
     */
    private fun initializePlayer(c: Context) {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        if (player == null) {
            player = ExoPlayer.Builder(c)
                .setAudioAttributes(audioAttributes, true)
                .build()
        }

        /**
         * Listens for changed playbackStates (e.g. pause, end)
         * Plays the next video when the current one ended
         */
        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(@Player.State state: Int) {
                val autoplay = PreferenceHelper.getBoolean(c, "autoplay", false)
                if (state == Player.STATE_ENDED) {
                    if (autoplay) playNextVideo(c)
                }
            }
        })
        setMediaItem(c)
    }

    /**
     * Plays the first related video to the current (used when the playback of the current video ended)
     */
    private fun playNextVideo(c: Context) {
        if (response!!.relatedStreams!!.isNotEmpty()) {
            val videoId = response!!
                .relatedStreams!![0].url!!
                .replace("/watch?v=", "")

            // destroy old player and its notification
            playerNotification = null
            player = null

            // kill old notification
            val notificationManager = c.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            notificationManager.cancel(1)

            // play new video on background
            playOnBackgroundMode(c, videoId)
        }
    }

    /**
     * Initializes the [playerNotification] attached to the [player] and shows it.
     */
    private fun initializePlayerNotification(c: Context) {
        playerNotification = PlayerNotificationManager
            .Builder(c, 1, "background_mode")
            // set the description of the notification
            .setMediaDescriptionAdapter(
                DescriptionAdapter(
                    response?.title!!,
                    response?.uploader!!,
                    response?.thumbnailUrl!!,
                    c
                )
            )
            .build()
        playerNotification?.apply {
            setPlayer(player)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setUseStopAction(true)
            setColorized(true)
            setMediaSessionToken(mediaSession.sessionToken)
        }
    }

    /**
     * Sets the [MediaItem] with the [response] into the [player]. Also creates a [MediaSessionConnector]
     * with the [mediaSession] and attach it to the [player].
     */
    private fun setMediaItem(c: Context) {
        response?.let {
            val mediaItem = MediaItem.Builder().setUri(it.hls!!).build()
            player?.setMediaItem(mediaItem)
        }

        mediaSession = MediaSessionCompat(c, this.javaClass.name)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
    }

    /**
     * Gets the video data and prepares the [player].
     */
    fun playOnBackgroundMode(
        c: Context,
        videoId: String,
        seekToPosition: Long = 0
    ) {
        runBlocking {
            val job = launch {
                response = RetrofitInstance.api.getStreams(videoId)
            }
            // Wait until the job is done, to load correctly later in the player
            job.join()

            initializePlayer(c)
            initializePlayerNotification(c)

            player?.apply {
                playWhenReady = playWhenReadyPlayer
                prepare()
            }

            if (!seekToPosition.equals(0)) player?.seekTo(seekToPosition)
        }
    }

    /**
     * Creates a singleton of this class, to not create a new [player] every time.
     */
    companion object {
        private var INSTANCE: BackgroundMode? = null

        fun getInstance(): BackgroundMode {
            if (INSTANCE == null) INSTANCE = BackgroundMode()
            return INSTANCE!!
        }
    }
}
