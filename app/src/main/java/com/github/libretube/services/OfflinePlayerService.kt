package com.github.libretube.services

import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.github.libretube.R
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.loadPlaybackParams
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.util.NowPlayingNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A service to play downloaded audio in the background
 */
class OfflinePlayerService : LifecycleService() {
    private var player: ExoPlayer? = null
    private var nowPlayingNotification: NowPlayingNotification? = null

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, BACKGROUND_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.playingOnBackground))
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .build()

        startForeground(PLAYER_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra(IntentData.videoId)

        lifecycleScope.launch(Dispatchers.IO) {
            val downloadWithItems = DatabaseHolder.Database.downloadDao().findById(videoId!!)
            withContext(Dispatchers.Main) {
                if (startAudioPlayer(downloadWithItems)) {
                    nowPlayingNotification = NowPlayingNotification(
                        this@OfflinePlayerService,
                        player!!,
                        true
                    )
                    val notificationData = PlayerNotificationData(
                        title = downloadWithItems.download.title,
                        uploaderName = downloadWithItems.download.uploader,
                        thumbnailPath = downloadWithItems.download.thumbnailPath
                    )
                    nowPlayingNotification?.updatePlayerNotification(videoId, notificationData)
                } else {
                    onDestroy()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Attempt to start an audio player with the given download items
     * @param downloadWithItem The database download to play from
     * @return whether starting the audio player succeeded
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun startAudioPlayer(downloadWithItem: DownloadWithItems): Boolean {
        player = ExoPlayer.Builder(this)
            .setUsePlatformDiagnostics(false)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(PlayerHelper.getAudioAttributes(), true)
            .setLoadControl(PlayerHelper.getLoadControl())
            .build()
            .loadPlaybackParams(isBackgroundMode = true).apply {
                playWhenReady = true
            }

        val audioItem = downloadWithItem.downloadItems.firstOrNull { it.type == FileType.AUDIO }
            ?: // in some rare cases, video files can contain audio
            downloadWithItem.downloadItems.firstOrNull { it.type == FileType.VIDEO } ?: return false

        val mediaItem = MediaItem.Builder()
            .setUri(audioItem.path.toAndroidUri())
            .build()

        player?.setMediaItem(mediaItem)
        player?.prepare()
        return true
    }

    override fun onDestroy() {
        nowPlayingNotification?.destroySelfAndPlayer()

        player = null
        nowPlayingNotification = null

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
