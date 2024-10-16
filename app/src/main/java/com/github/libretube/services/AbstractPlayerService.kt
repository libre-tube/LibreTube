package com.github.libretube.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.LibreTubeApp.Companion.PLAYER_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.enums.NotificationId
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PauseableTimer
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.launch

@UnstableApi
abstract class AbstractPlayerService : LifecycleService() {
    var player: ExoPlayer? = null
    var nowPlayingNotification: NowPlayingNotification? = null
    var trackSelector: DefaultTrackSelector? = null

    lateinit var videoId: String
    var isTransitioning = true

    val handler = Handler(Looper.getMainLooper())

    private val binder = LocalBinder()

    /**
     * Listener for passing playback state changes to the AudioPlayerFragment
     */
    var onStateOrPlayingChanged: ((isPlaying: Boolean) -> Unit)? = null
    var onNewVideoStarted: ((streamItem: StreamItem) -> Unit)? = null

    private val watchPositionTimer = PauseableTimer(
        onTick = ::saveWatchPosition,
        delayMillis = PlayerHelper.WATCH_POSITION_TIMER_DELAY_MS
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)

            // Start or pause watch position timer
            if (isPlaying) {
                watchPositionTimer.resume()
            } else {
                watchPositionTimer.pause()
            }

            onStateOrPlayingChanged?.let { it(isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            onStateOrPlayingChanged?.let { it(player?.isPlaying ?: false) }

            this@AbstractPlayerService.onPlaybackStateChanged(playbackState)
        }

        override fun onPlayerError(error: PlaybackException) {
            // show a toast on errors
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    error.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

            if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                PlayerHelper.setPreferredAudioQuality(this@AbstractPlayerService, player, trackSelector ?: return)
            }
        }
    }

    private val playerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.serializableExtra<PlayerEvent>(PlayerHelper.CONTROL_TYPE) ?: return
            val player = player ?: return

            if (PlayerHelper.handlePlayerAction(player, event)) return

            when (event) {
                PlayerEvent.Next -> {
                    PlayingQueue.navigateNext()
                }
                PlayerEvent.Prev -> {
                    PlayingQueue.navigatePrev()
                }
                PlayerEvent.Stop -> {
                    onDestroy()
                }
                else -> Unit
            }
        }
    }

    abstract val isOfflinePlayer: Boolean

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, PLAYER_CHANNEL_NAME)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.playingOnBackground))
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .build()

        startForeground(NotificationId.PLAYER_PLAYBACK.id, notification)

        ContextCompat.registerReceiver(
            this,
            playerActionReceiver,
            IntentFilter(PlayerHelper.getIntentActionName(this)),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PlayingQueue.resetToDefaults()

        lifecycleScope.launch {
            if (intent != null) {
                createPlayerAndNotification()
                onServiceCreated(intent)
                startPlaybackAndUpdateNotification()
            }
            else stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    abstract suspend fun onServiceCreated(intent: Intent)

    @OptIn(UnstableApi::class)
    private fun createPlayerAndNotification() {
        val trackSelector = DefaultTrackSelector(this)
        this.trackSelector = trackSelector

        trackSelector.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        }

        player = PlayerHelper.createPlayer(this, trackSelector, true)
        // prevent android from putting LibreTube to sleep when locked
        player!!.setWakeMode(C.WAKE_MODE_LOCAL)
        player!!.addListener(playerListener)

        PlayerHelper.setPreferredCodecs(trackSelector)

        nowPlayingNotification = NowPlayingNotification(
            this,
            player!!,
            backgroundOnly = true,
            offlinePlayer = isOfflinePlayer
        )
    }

    abstract suspend fun startPlaybackAndUpdateNotification()

    fun saveWatchPosition() {
        if (isTransitioning || !PlayerHelper.watchPositionsVideo) return

        player?.let { PlayerHelper.saveWatchPosition(it, videoId) }
    }

    override fun onDestroy() {
        PlayingQueue.resetToDefaults()

        saveWatchPosition()

        nowPlayingNotification?.destroySelf()
        nowPlayingNotification = null
        watchPositionTimer.destroy()

        handler.removeCallbacksAndMessages(null)

        runCatching {
            player?.stop()
            player?.release()
        }
        player = null

        runCatching {
            unregisterReceiver(playerActionReceiver)
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        super.onDestroy()
    }

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }

    abstract fun onPlaybackStateChanged(playbackState: Int)

    abstract fun getChapters(): List<ChapterSegment>

    fun getCurrentPosition() = player?.currentPosition

    fun getDuration() = player?.duration

    fun seekToPosition(position: Long) = player?.seekTo(position)

    inner class LocalBinder : Binder() {
        // Return this instance of [AbstractPlayerService] so clients can call public methods
        fun getService(): AbstractPlayerService = this@AbstractPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
}
