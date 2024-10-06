package com.github.libretube.services

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PlayerNotificationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

/**
 * A service to play downloaded audio in the background
 */
@UnstableApi
class OfflinePlayerService : AbstractPlayerService() {
    private var downloadsWithItems: List<DownloadWithItems> = emptyList()

    override suspend fun onServiceCreated(intent: Intent) {
        downloadsWithItems = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.downloadDao().getAll()
        }
        if (downloadsWithItems.isEmpty()) {
            onDestroy()
            return
        }

        val videoId = intent.getStringExtra(IntentData.videoId)

        val downloadToPlay = if (videoId == null) {
            downloadsWithItems = downloadsWithItems.shuffled()
            downloadsWithItems.first()
        } else {
            downloadsWithItems.first { it.download.videoId == videoId }
        }

        this@OfflinePlayerService.videoId = downloadToPlay.download.videoId
    }

    /**
     * Attempt to start an audio player with the given download items
     */
    override suspend fun startPlaybackAndUpdateNotification() {
        val downloadWithItems = downloadsWithItems.firstOrNull { it.download.videoId == videoId }
        if (downloadWithItems == null) {
            stopSelf()
            return
        }

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

        if (audioItem == null) {
            stopSelf()
            return
        }

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

    override fun onPlaybackStateChanged(playbackState: Int) {
        // automatically go to the next video/audio when the current one ended
        if (playbackState == Player.STATE_ENDED) {
            val currentIndex = downloadsWithItems.indexOfFirst { it.download.videoId == videoId }
            downloadsWithItems.getOrNull(currentIndex + 1)?.let {
                this@OfflinePlayerService.videoId = it.download.videoId

                lifecycleScope.launch {
                    startPlaybackAndUpdateNotification()
                }
            }
        }
    }
}
