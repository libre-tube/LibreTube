package com.github.libretube.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.BACKGROUND_CHANNEL_ID
import com.github.libretube.PLAYER_NOTIFICATION_ID
import com.github.libretube.R
import com.github.libretube.obj.Segment
import com.github.libretube.obj.Segments
import com.github.libretube.obj.Streams
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.AutoPlayHelper
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PlayerHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.toID
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
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
     *PlaylistId for autoplay
     */
    private var playlistId: String? = null

    /**
     * The response that gets when called the Api.
     */
    private var streams: Streams? = null

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    private var player: ExoPlayer? = null
    private var playWhenReadyPlayer = true

    /**
     * The [AudioAttributes] handle the audio focus of the [player]
     */
    private lateinit var audioAttributes: AudioAttributes

    /**
     * SponsorBlock Segment data
     */
    private var segmentData: Segments? = null

    /**
     * [Notification] for the player
     */
    private lateinit var nowPlayingNotification: NowPlayingNotification

    /**
     * The [videoId] of the next stream for autoplay
     */
    private lateinit var nextStreamId: String

    /**
     * Helper for finding the next video in the playlist
     */
    private lateinit var autoPlayHelper: AutoPlayHelper

    /**
     * Setting the required [notification] for running as a foreground service
     */
    override fun onCreate() {
        super.onCreate()
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
        try {
            // get the intent arguments
            videoId = intent?.getStringExtra("videoId")!!
            playlistId = intent.getStringExtra("playlistId")
            val position = intent.getLongExtra("position", 0L)

            // initialize the playlist autoPlay Helper
            if (playlistId != null) autoPlayHelper = AutoPlayHelper(playlistId!!)

            // play the audio in the background
            playAudio(videoId, position)
        } catch (e: Exception) {
            stopForeground(true)
            stopSelf()
        }
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
                streams = RetrofitInstance.api.getStreams(videoId)
            }
            // Wait until the job is done, to load correctly later in the player
            job.join()

            initializePlayer()
            setMediaItem()

            // create the notification
            if (!this@BackgroundMode::nowPlayingNotification.isInitialized) {
                nowPlayingNotification = NowPlayingNotification(this@BackgroundMode, player!!)
            }
            nowPlayingNotification.updatePlayerNotification(streams!!)

            player?.apply {
                playWhenReady = playWhenReadyPlayer
                prepare()
            }

            // seek to the previous position if available
            if (seekToPosition != 0L) player?.seekTo(seekToPosition)

            // set the playback speed
            val playbackSpeed = PreferenceHelper.getString(
                PreferenceKeys.BACKGROUND_PLAYBACK_SPEED,
                "1"
            ).toFloat()
            player?.setPlaybackSpeed(playbackSpeed)

            fetchSponsorBlockSegments()

            setNextStream()
        }
    }

    /**
     * create the player
     */
    private fun initializePlayer() {
        if (player != null) return

        audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

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
                        onDestroy()
                    }
                }
            }
        })
    }

    /**
     * set the videoId of the next stream for autoplay
     */
    private fun setNextStream() {
        if (streams!!.relatedStreams!!.isNotEmpty()) {
            nextStreamId = streams?.relatedStreams!![0].url.toID()
        }

        if (playlistId == null) return
        if (!this::autoPlayHelper.isInitialized) autoPlayHelper = AutoPlayHelper(playlistId!!)
        // search for the next videoId in the playlist
        CoroutineScope(Dispatchers.IO).launch {
            val nextId = autoPlayHelper.getNextPlaylistVideoId(videoId)
            if (nextId != null) nextStreamId = nextId
        }
    }

    /**
     * Plays the first related video to the current (used when the playback of the current video ended)
     */
    private fun playNextVideo() {
        if (!this::nextStreamId.isInitialized || nextStreamId == videoId) return

        // play new video on background
        this.videoId = nextStreamId
        this.segmentData = null
        playAudio(videoId)
    }

    /**
     * Sets the [MediaItem] with the [streams] into the [player]. Also creates a [MediaSessionConnector]
     * with the [mediaSession] and attach it to the [player].
     */
    private fun setMediaItem() {
        streams?.let {
            val mediaItem = MediaItem.Builder().setUri(it.hls!!).build()
            player?.setMediaItem(mediaItem)
        }
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
                if (PreferenceHelper.getBoolean(
                        "sb_notifications_key",
                        true
                    )
                ) {
                    try {
                        Toast.makeText(this, R.string.segment_skipped, Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        // Do nothing.
                    }
                }
                player?.seekTo(segmentEnd)
            }
        }
    }

    /**
     * destroy the [BackgroundMode] foreground service
     */
    override fun onDestroy() {
        // called when the user pressed stop in the notification
        // stop the service from being in the foreground and remove the notification
        stopForeground(true)
        // destroy the service
        stopSelf()
        if (this::nowPlayingNotification.isInitialized) nowPlayingNotification.destroy()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
