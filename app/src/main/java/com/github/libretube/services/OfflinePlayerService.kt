package com.github.libretube.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.LibreTubeApp.Companion.PLAYER_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.enums.NotificationId
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PauseableTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

/**
 * A service to play downloaded audio in the background
 */
class OfflinePlayerService : LifecycleService() {
    private var player: ExoPlayer? = null
    private var nowPlayingNotification: NowPlayingNotification? = null
    private lateinit var videoId: String
    private var downloadsWithItems: List<DownloadWithItems> = emptyList()

    private val watchPositionTimer = PauseableTimer(
        onTick = this::saveWatchPosition,
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
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            // automatically go to the next video/audio when the current one ended
            if (playbackState == Player.STATE_ENDED) {
                val currentIndex = downloadsWithItems.indexOfFirst { it.download.videoId == videoId }
                downloadsWithItems.getOrNull(currentIndex + 1)?.let {
                    this@OfflinePlayerService.videoId = it.download.videoId
                    startAudioPlayer(it)
                }
            }
        }
    }

    private val playerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.serializableExtra<PlayerEvent>(PlayerHelper.CONTROL_TYPE) ?: return
            val player = player ?: return

            if (PlayerHelper.handlePlayerAction(player, event)) return

            when (event) {
                PlayerEvent.Stop -> onDestroy()
                else -> Unit
            }
        }
    }

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
        lifecycleScope.launch {
            downloadsWithItems = withContext(Dispatchers.IO) {
                DatabaseHolder.Database.downloadDao().getAll()
            }
            if (downloadsWithItems.isEmpty()) {
                onDestroy()
                return@launch
            }

            val videoId = intent?.getStringExtra(IntentData.videoId)

            val downloadToPlay = if (videoId == null) {
                downloadsWithItems = downloadsWithItems.shuffled()
                downloadsWithItems.first()
            } else {
                downloadsWithItems.first { it.download.videoId == videoId }
            }

            this@OfflinePlayerService.videoId = downloadToPlay.download.videoId

            createPlayerAndNotification()

            // destroy the service if there was no success playing the selected audio/video
            if (!startAudioPlayer(downloadToPlay)) onDestroy()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    private fun createPlayerAndNotification() {
        val trackSelector = DefaultTrackSelector(this@OfflinePlayerService)
        trackSelector.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        }

        player = PlayerHelper.createPlayer(this@OfflinePlayerService, trackSelector, true)
        // prevent android from putting LibreTube to sleep when locked
        player!!.setWakeMode(C.WAKE_MODE_LOCAL)
        player!!.addListener(playerListener)

        nowPlayingNotification = NowPlayingNotification(
            this,
            player!!,
            NowPlayingNotification.Companion.NowPlayingNotificationType.AUDIO_OFFLINE
        )
    }

    /**
     * Attempt to start an audio player with the given download items
     * @param downloadWithItems The database download to play from
     * @return whether starting the audio player succeeded
     */
    private fun startAudioPlayer(downloadWithItems: DownloadWithItems): Boolean {
        val notificationData = PlayerNotificationData(
            title = downloadWithItems.download.title,
            uploaderName = downloadWithItems.download.uploader,
            thumbnailPath = downloadWithItems.download.thumbnailPath
        )
        nowPlayingNotification?.updatePlayerNotification(videoId, notificationData)

        val audioItem = downloadWithItems.downloadItems.filter { it.path.exists() }
            .firstOrNull { it.type == FileType.AUDIO }
            ?: // in some rare cases, video files can contain audio
            downloadWithItems.downloadItems.firstOrNull { it.type == FileType.VIDEO }
            ?: return false

        val mediaItem = MediaItem.Builder()
            .setUri(audioItem.path.toAndroidUri())
            .build()

        player?.setMediaItem(mediaItem)
        player?.playWhenReady = PlayerHelper.playAutomatically
        player?.prepare()

        if (PlayerHelper.watchPositionsAudio) {
            PlayerHelper.getStoredWatchPosition(videoId, downloadWithItems.download.duration)?.let {
                player?.seekTo(it)
            }
        }

        return true
    }

    private fun saveWatchPosition() {
        if (!PlayerHelper.watchPositionsVideo) return

        player?.let { PlayerHelper.saveWatchPosition(it, videoId) }
    }

    override fun onDestroy() {
        saveWatchPosition()

        nowPlayingNotification?.destroySelf()
        nowPlayingNotification = null
        watchPositionTimer.destroy()

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

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }
}
