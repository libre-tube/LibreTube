package com.github.libretube.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.BACKGROUND_CHANNEL_ID
import com.github.libretube.PLAYER_NOTIFICATION_ID
import com.github.libretube.R
import com.github.libretube.obj.Segment
import com.github.libretube.obj.Segments
import com.github.libretube.obj.Streams
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.DescriptionAdapter
import com.github.libretube.util.PlayerHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.toID
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Loads the selected videos audio in background mode with a notification area.
 */
class BackgroundMode : Service() {
    /**
     * VideoId of the video
     */
    private lateinit var videoId: String

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
     * SponsorBlock Segment data
     */
    private var segmentData: Segments? = null

    override fun onCreate() {
        super.onCreate()
        /**
         * setting the required notification for running as a foreground service
         */
        if (Build.VERSION.SDK_INT >= 26) {
            val channelId = BACKGROUND_CHANNEL_ID
            val channel = NotificationChannel(
                channelId,
                "Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.playingOnBackground)).build()
            startForeground(PLAYER_NOTIFICATION_ID, notification)
        }
    }

    /**
     * Initializes the [player] with the [MediaItem].
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // destroy the old player
        destroyPlayer()

        // get the intent arguments
        videoId = intent?.getStringExtra("videoId")!!
        val position = intent.getLongExtra("position", 0L)

        // play the audio in the background
        playAudio(videoId, position)
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Gets the video data and prepares the [player].
     */
    private fun playAudio(
        videoId: String,
        seekToPosition: Long = 0
    ) {
        runBlocking {
            val job = launch {
                response = RetrofitInstance.api.getStreams(videoId)
            }
            // Wait until the job is done, to load correctly later in the player
            job.join()

            initializePlayer()
            initializePlayerNotification()

            player?.apply {
                playWhenReady = playWhenReadyPlayer
                prepare()
            }

            // seek to the previous position if available
            if (seekToPosition != 0L) player?.seekTo(seekToPosition)

            fetchSponsorBlockSegments()
        }
    }

    /**
     * create the player
     */
    private fun initializePlayer() {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        if (player == null) {
            player = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build()
        }

        /**
         * Listens for changed playbackStates (e.g. pause, end)
         * Plays the next video when the current one ended
         */
        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(@Player.State state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        val autoplay = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_PLAY, true)
                        if (autoplay) playNextVideo()
                    }
                    Player.STATE_IDLE -> {
                        // called when the user pressed stop in the notification
                        // stop the service from being in the foreground and remove the notification
                        stopForeground(true)
                        // destroy the service
                        stopSelf()
                    }
                }
            }
        })
        setMediaItem()
    }

    /**
     * Plays the first related video to the current (used when the playback of the current video ended)
     */
    private fun playNextVideo() {
        if (response!!.relatedStreams!!.isNotEmpty()) {
            val videoId = response!!
                .relatedStreams!![0].url.toID()

            // destroy previous notification and player
            destroyPlayer()

            // play new video on background
            this.videoId = videoId
            this.segmentData = null
            playAudio(videoId)
        }
    }

    /**
     * Initializes the [playerNotification] attached to the [player] and shows it.
     */
    private fun initializePlayerNotification() {
        playerNotification = PlayerNotificationManager
            .Builder(this, PLAYER_NOTIFICATION_ID, BACKGROUND_CHANNEL_ID)
            // set the description of the notification
            .setMediaDescriptionAdapter(
                DescriptionAdapter(
                    response?.title!!,
                    response?.uploader!!,
                    response?.thumbnailUrl!!,
                    this
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
    private fun setMediaItem() {
        response?.let {
            val mediaItem = MediaItem.Builder().setUri(it.hls!!).build()
            player?.setMediaItem(mediaItem)
        }

        mediaSession = MediaSessionCompat(this, this.javaClass.name)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() {
        CoroutineScope(Dispatchers.IO).launch {
            kotlin.runCatching {
                val categories = PlayerHelper.getSponsorBlockCategories()
                if (categories.size > 0) {
                    segmentData =
                        RetrofitInstance.api.getSegments(
                            videoId,
                            ObjectMapper().writeValueAsString(categories)
                        )
                    checkForSegments()
                }
            }
        }
    }

    /**
     * check for SponsorBlock segments
     */
    private fun checkForSegments() {
        Handler(Looper.getMainLooper()).postDelayed(this::checkForSegments, 100)

        if (segmentData == null || segmentData!!.segments.isEmpty()) return

        segmentData!!.segments.forEach { segment: Segment ->
            val segmentStart = (segment.segment!![0] * 1000f).toLong()
            val segmentEnd = (segment.segment[1] * 1000f).toLong()
            val currentPosition = player?.currentPosition
            if (currentPosition in segmentStart until segmentEnd) {
                player?.seekTo(segmentEnd)
            }
        }
    }

    private fun destroyPlayer() {
        // clear old player and its notification
        playerNotification = null
        player = null

        // kill old notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        notificationManager.cancel(1)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
