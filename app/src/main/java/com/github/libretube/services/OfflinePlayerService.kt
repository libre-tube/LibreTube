package com.github.libretube.services

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.activities.NoInternetActivity
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

/**
 * A service to play downloaded audio in the background
 */
@OptIn(UnstableApi::class)
open class OfflinePlayerService : AbstractPlayerService() {
    override val isOfflinePlayer: Boolean = true
    override val isAudioOnlyPlayer: Boolean = true
    private var noInternetService: Boolean = false

    private var downloadWithItems: DownloadWithItems? = null
    private lateinit var downloadTab: DownloadTab
    private var shuffle: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main)

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && PlayerHelper.isAutoPlayEnabled()) {
                playNextVideo(PlayingQueue.getNext() ?: return)
            }

            if (playbackState == Player.STATE_READY) {
                scope.launch(Dispatchers.IO) {
                    val watchHistoryItem = downloadWithItems?.download?.toStreamItem()?.toWatchHistoryItem(videoId)
                    if (watchHistoryItem != null) {
                        DatabaseHelper.addToWatchHistory(watchHistoryItem)
                    }
                }
            }
        }
    }

    override suspend fun onServiceCreated(args: Bundle) {
        if (args.isEmpty) return

        downloadTab = args.serializable(IntentData.downloadTab)!!
        shuffle = args.getBoolean(IntentData.shuffle, false)
        noInternetService = args.getBoolean(IntentData.noInternet, false)

        val videoId = if (shuffle) {
            runBlocking(Dispatchers.IO) {
                Database.downloadDao().getRandomVideoIdByFileType(FileType.AUDIO)
            }
        } else {
            args.getString(IntentData.videoId)
        } ?: return
        setVideoId(videoId)

        PlayingQueue.clear()

        exoPlayer?.addListener(playerListener)

        fillQueue()
    }

    override fun getIntentActivity(): Class<*> {
        return if (noInternetService) NoInternetActivity::class.java else MainActivity::class.java
    }

    /**
     * Attempt to start an audio player with the given download items
     */
    override suspend fun startPlayback() {
        super.startPlayback()

        val downloadWithItems = withContext(Dispatchers.IO) {
            Database.downloadDao().findById(videoId)
        }!!
        this.downloadWithItems = downloadWithItems

        PlayingQueue.updateCurrent(downloadWithItems.download.toStreamItem())

        withContext(Dispatchers.Main) {
            setMediaItem(downloadWithItems)
            exoPlayer?.playWhenReady = PlayerHelper.playAutomatically
            exoPlayer?.prepare()

            if (watchPositionsEnabled) {
                DatabaseHelper.getWatchPosition(videoId)?.let {
                    if (!DatabaseHelper.isVideoWatched(
                            it,
                            downloadWithItems.download.duration
                        )
                    ) exoPlayer?.seekTo(it)
                }
            }
        }
    }

    open fun setMediaItem(downloadWithItems: DownloadWithItems) {
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
            .setMetadata(downloadWithItems)
            .build()

        exoPlayer?.setMediaItem(mediaItem)
    }

    private suspend fun fillQueue() {
        val downloads = withContext(Dispatchers.IO) {
            Database.downloadDao().getAll()
        }
            .filterByTab(downloadTab)
            .toMutableList()

        if (shuffle) downloads.shuffle()

        PlayingQueue.insertRelatedStreams(downloads.map { it.download.toStreamItem() })
    }

    private fun playNextVideo(videoId: String) {
        setVideoId(videoId)

        scope.launch {
            startPlayback()
        }
    }

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }
}
