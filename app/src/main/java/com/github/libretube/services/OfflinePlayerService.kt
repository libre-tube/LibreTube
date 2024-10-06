package com.github.libretube.services

import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadChapter
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

/**
 * A service to play downloaded audio in the background
 */
@UnstableApi
class OfflinePlayerService : AbstractPlayerService() {
    private var downloadWithItems: DownloadWithItems? = null

    override suspend fun onServiceCreated(intent: Intent) {
        videoId = intent.getStringExtra(IntentData.videoId) ?: return

        PlayingQueue.clear()

        PlayingQueue.setOnQueueTapListener { streamItem ->
            streamItem.url?.toID()?.let { playNextVideo(it) }
        }

        fillQueue()
    }

    /**
     * Attempt to start an audio player with the given download items
     */
    override suspend fun startPlaybackAndUpdateNotification() {
        val downloadWithItems = withContext(Dispatchers.IO) {
            Database.downloadDao().findById(videoId)
        }
        this.downloadWithItems = downloadWithItems
        onNewVideoStarted?.let { it(downloadWithItems.download.toStreamItem()) }

        PlayingQueue.updateCurrent(downloadWithItems.download.toStreamItem())

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

    private suspend fun fillQueue() {
        val downloads = withContext(Dispatchers.IO) {
            Database.downloadDao().getAll()
        }

        PlayingQueue.insertRelatedStreams(downloads.map { it.download.toStreamItem() })
    }

    private fun playNextVideo(videoId: String) {
        saveWatchPosition()

        this.videoId = videoId

        lifecycleScope.launch {
            startPlaybackAndUpdateNotification()
        }
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
        if (playbackState == Player.STATE_ENDED && PlayerHelper.isAutoPlayEnabled()) {
            playNextVideo(PlayingQueue.getNext() ?: return)
        }
    }

    override fun getChapters(): List<ChapterSegment> =
        downloadWithItems?.downloadChapters.orEmpty().map(DownloadChapter::toChapterSegment)
}
