package com.github.libretube.services

import android.app.Notification
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.LibreTubeApp.Companion.PLAYER_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.extensions.parcelableExtra
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.checkForSegments
import com.github.libretube.helpers.PlayerHelper.loadPlaybackParams
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.NowPlayingNotification.Companion.PLAYER_NOTIFICATION_ID
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/**
 * Loads the selected videos audio in background mode with a notification area.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OnlinePlayerService : LifecycleService() {
    /**
     * VideoId of the video
     */
    private lateinit var videoId: String

    /**
     * PlaylistId/ChannelId for autoplay
     */
    private var playlistId: String? = null
    private var channelId: String? = null

    /**
     * The response that gets when called the Api.
     */
    var streams: Streams? = null
        private set

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    var player: ExoPlayer? = null
    private var isTransitioning = true

    /**
     * SponsorBlock Segment data
     */
    private var segments = listOf<Segment>()
    private var sponsorBlockConfig = PlayerHelper.getSponsorBlockCategories()

    /**
     * [Notification] for the player
     */
    private lateinit var nowPlayingNotification: NowPlayingNotification

    /**
     * Autoplay Preference
     */
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Used for connecting to the AudioPlayerFragment
     */
    private val binder = LocalBinder()

    /**
     * Listener for passing playback state changes to the AudioPlayerFragment
     */
    var onIsPlayingChanged: ((isPlaying: Boolean) -> Unit)? = null
    var onNewVideo: ((streams: Streams, videoId: String) -> Unit)? = null

    /**
     * Setting the required [Notification] for running as a foreground service
     */
    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, PLAYER_CHANNEL_NAME)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.playingOnBackground))
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .build()

        startForeground(PLAYER_NOTIFICATION_ID, notification)
    }

    /**
     * Initializes the [player] with the [MediaItem].
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // reset the playing queue listeners
        PlayingQueue.resetToDefaults()

        intent?.parcelableExtra<PlayerData>(IntentData.playerData)?.let { playerData ->
            // get the intent arguments
            videoId = playerData.videoId
            playlistId = playerData.playlistId

            // play the audio in the background
            loadAudio(playerData)

            PlayingQueue.setOnQueueTapListener { streamItem ->
                streamItem.url?.toID()?.let { playNextVideo(it) }
            }

            if (PlayerHelper.watchPositionsAudio) {
                updateWatchPosition()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWatchPosition() {
        player?.currentPosition?.let {
            if (isTransitioning) return@let

            val watchPosition = WatchPosition(videoId, it)

            CoroutineScope(Dispatchers.IO).launch {
                Database.watchPositionDao().insert(watchPosition)
            }
        }
        handler.postDelayed(this::updateWatchPosition, 500)
    }

    /**
     * Gets the video data and prepares the [player].
     */
    private fun loadAudio(playerData: PlayerData) {
        val (videoId, _, _, keepQueue, timestamp) = playerData
        isTransitioning = true

        lifecycleScope.launch(Dispatchers.IO) {
            streams = runCatching {
                RetrofitInstance.api.getStreams(videoId)
            }.getOrNull() ?: return@launch

            // clear the queue if it shouldn't be kept explicitly
            if (!keepQueue) PlayingQueue.clear()

            if (PlayingQueue.isEmpty()) {
                PlayingQueue.updateQueue(streams!!.toStreamItem(videoId), playlistId, channelId)
                insertRelatedStreamsToQueue()
            } else if (PlayingQueue.isLast() && playlistId == null && channelId == null) {
                insertRelatedStreamsToQueue()
            }

            // save the current stream to the queue
            streams?.toStreamItem(videoId)?.let {
                PlayingQueue.updateCurrent(it)
            }

            withContext(Dispatchers.Main) {
                playAudio(timestamp)
            }
        }
    }

    private fun playAudio(seekToPosition: Long) {
        initializePlayer()
        lifecycleScope.launch(Dispatchers.IO) {
            setMediaItem()

            withContext(Dispatchers.Main) {
                // seek to the previous position if available
                if (seekToPosition != 0L) {
                    player?.seekTo(seekToPosition)
                } else if (PlayerHelper.watchPositionsAudio) {
                    PlayerHelper.getPosition(videoId, streams?.duration)?.let {
                        player?.seekTo(it)
                    }
                }
            }
        }

        // create the notification
        if (!this@OnlinePlayerService::nowPlayingNotification.isInitialized) {
            nowPlayingNotification = NowPlayingNotification(
                this@OnlinePlayerService,
                player!!,
                true
            )
        }
        val playerNotificationData = PlayerNotificationData(
            streams?.title,
            streams?.uploader,
            streams?.thumbnailUrl
        )
        nowPlayingNotification.updatePlayerNotification(videoId, playerNotificationData)
        streams?.let { onNewVideo?.invoke(it, videoId) }

        player?.apply {
            playWhenReady = PlayerHelper.playAutomatically
            prepare()
        }

        if (PlayerHelper.sponsorBlockEnabled) fetchSponsorBlockSegments()
    }

    /**
     * create the player
     */
    private fun initializePlayer() {
        if (player != null) return

        val trackSelector = DefaultTrackSelector(this)
        PlayerHelper.applyPreferredAudioQuality(this, trackSelector)
        trackSelector.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        }

        player = ExoPlayer.Builder(this)
            .setUsePlatformDiagnostics(false)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(PlayerHelper.getAudioAttributes(), true)
            .setLoadControl(PlayerHelper.getLoadControl())
            .setTrackSelector(trackSelector)
            .build()
            .loadPlaybackParams(isBackgroundMode = true)

        /**
         * Listens for changed playbackStates (e.g. pause, end)
         * Plays the next video when the current one ended
         */
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                onIsPlayingChanged?.invoke(isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        if (PlayerHelper.autoPlayEnabled && !isTransitioning) playNextVideo()
                    }

                    Player.STATE_IDLE -> {
                        onDestroy()
                    }

                    Player.STATE_BUFFERING -> {}
                    Player.STATE_READY -> {
                        isTransitioning = false

                        // save video to watch history when the video starts playing or is being resumed
                        // waiting for the player to be ready since the video can't be claimed to be watched
                        // while it did not yet start actually, but did buffer only so far
                        lifecycleScope.launch(Dispatchers.IO) {
                            streams?.let { DatabaseHelper.addToWatchHistory(videoId, it) }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // show a toast on errors
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        this@OnlinePlayerService.applicationContext,
                        error.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    /**
     * Plays the next video from the queue
     */
    private fun playNextVideo(nextId: String? = null) {
        val nextVideo = nextId ?: PlayingQueue.getNext() ?: return

        // play new video on background
        this.videoId = nextVideo
        this.streams = null
        this.segments = emptyList()
        loadAudio(PlayerData(videoId, keepQueue = true))
    }

    /**
     * Sets the [MediaItem] with the [streams] into the [player]
     */
    private suspend fun setMediaItem() {
        val streams = streams ?: return

        val (uri, mimeType) = if (streams.audioStreams.isNotEmpty()) {
            val disableProxy = ProxyHelper.useYouTubeSourceWithoutProxy(
                streams.videoStreams.first().url!!
            )
            PlayerHelper.createDashSource(
                streams,
                this,
                disableProxy
            ) to MimeTypes.APPLICATION_MPD
        } else {
            ProxyHelper.unwrapStreamUrl(streams.hls.orEmpty()).toUri() to MimeTypes.APPLICATION_M3U8
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setMetadata(streams)
            .build()
        withContext(Dispatchers.Main) { player?.setMediaItem(mediaItem) }
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (sponsorBlockConfig.isEmpty()) return@runCatching
                segments = RetrofitInstance.api.getSegments(
                    videoId,
                    JsonHelper.json.encodeToString(sponsorBlockConfig.keys)
                ).segments
                checkForSegments()
            }
        }
    }

    /**
     * check for SponsorBlock segments
     */
    private fun checkForSegments() {
        handler.postDelayed(this::checkForSegments, 100)

        player?.checkForSegments(this, segments, sponsorBlockConfig)
    }

    private fun insertRelatedStreamsToQueue() {
        if (!PlayerHelper.autoInsertRelatedVideos) return
        streams?.relatedStreams?.toTypedArray()?.let {
            PlayingQueue.add(*it, skipExisting = true)
        }
    }

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }

    /**
     * destroy the [OnlinePlayerService] foreground service
     */
    override fun onDestroy() {
        // reset the playing queue
        PlayingQueue.resetToDefaults()

        if (this::nowPlayingNotification.isInitialized) nowPlayingNotification.destroySelfAndPlayer()

        // called when the user pressed stop in the notification
        // stop the service from being in the foreground and remove the notification
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // destroy the service
        stopSelf()

        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of [BackgroundMode] so clients can call public methods
        fun getService(): OnlinePlayerService = this@OnlinePlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun getCurrentPosition() = player?.currentPosition

    fun getDuration() = player?.duration

    fun seekToPosition(position: Long) = player?.seekTo(position)

    fun pause() {
        player?.pause()
    }

    fun play() {
        player?.play()
    }
}
