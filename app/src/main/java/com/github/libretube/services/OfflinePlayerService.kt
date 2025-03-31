package com.github.libretube.services

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.updateParameters
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
    override var isAudioOnlyPlayer: Boolean = true
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
        isAudioOnlyPlayer = args.getBoolean(IntentData.audioOnly, false)

        val videoId = if (shuffle) {
            runBlocking(Dispatchers.IO) {
                Database.downloadDao().getRandomVideoIdByFileType(FileType.AUDIO)
            }
        } else {
            args.getString(IntentData.videoId)
        } ?: return
        setVideoId(videoId)

        exoPlayer?.addListener(playerListener)
        trackSelector?.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudioOnlyPlayer)
        }

        PlayingQueue.clear()
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

    private fun setMediaItem(downloadWithItems: DownloadWithItems) {
        val downloadFiles = downloadWithItems.downloadItems.filter { it.path.exists() }

        val videoUri = downloadFiles.firstOrNull { it.type == FileType.VIDEO }?.path?.toAndroidUri()
        val audioUri = downloadFiles.firstOrNull { it.type == FileType.AUDIO }?.path?.toAndroidUri()
        if (isAudioOnlyPlayer && audioUri == null) {
            stopSelf()
            return
        }

        val subtitleInfo = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

        val subtitle = subtitleInfo?.let {
            SubtitleConfiguration.Builder(it.path.toAndroidUri())
                .setMimeType(MimeTypes.APPLICATION_TTML)
                .setLanguage(it.language ?: "en")
                .build()
        }

        when {
            videoUri != null && audioUri != null -> {
                val videoItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()

                val videoSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(videoItem)

                val audioSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(audioUri))

                var mediaSource = MergingMediaSource(audioSource, videoSource)
                if (subtitle != null) {
                    val subtitleSource = SingleSampleMediaSource.Factory(FileDataSource.Factory())
                        .createMediaSource(subtitle, C.TIME_UNSET)

                    mediaSource = MergingMediaSource(mediaSource, subtitleSource)
                }

                exoPlayer?.setMediaSource(mediaSource)
            }

            videoUri != null -> exoPlayer?.setMediaItem(
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )

            audioUri != null -> exoPlayer?.setMediaItem(
                MediaItem.Builder()
                    .setUri(audioUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )
        }

        trackSelector?.updateParameters {
            setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
            setPreferredTextLanguage(subtitle?.language)
        }
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
