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
import androidx.media3.exoplayer.source.MediaSource
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
import com.github.libretube.ui.fragments.DownloadSortingOrder
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.ui.fragments.DownloadsFragmentPage.Companion.sortDownloadList
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
    private var noInternetService: Boolean = false

    private var downloadWithItems: DownloadWithItems? = null
    private lateinit var downloadTab: DownloadTab
    private var shuffle: Boolean = false
    private var playlistId: String? = null
    private var downloadSortOrder: DownloadSortingOrder? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                playNextVideo()
            }

            // add video to watch history when playback starts
            if (playbackState == Player.STATE_READY && PlayerHelper.watchHistoryEnabled) {
                scope.launch(Dispatchers.IO) {
                    val watchHistoryItem =
                        downloadWithItems?.download?.toStreamItem()?.toWatchHistoryItem(videoId)
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
        playlistId = args.getString(IntentData.playlistId)
        downloadSortOrder = args.serializable(IntentData.sortOptions)

        PlayingQueue.clear()

        this.videoId = if (shuffle) {
            runBlocking(Dispatchers.IO) {
                if (downloadTab == DownloadTab.PLAYLIST) {
                    Database.downloadDao()
                        .getDownloadPlaylistById(playlistId!!).downloadVideos.randomOrNull()
                } else {
                    Database.downloadDao().getAll().filterByTab(downloadTab)
                        .randomOrNull()?.download
                }
            }?.videoId
        } else {
            args.getString(IntentData.videoId)
        } ?: return

        exoPlayer?.addListener(playerListener)
        trackSelector?.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudioOnlyPlayer)
        }

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
        } ?: return
        this.downloadWithItems = downloadWithItems

        PlayingQueue.updateCurrent(downloadWithItems.download.toStreamItem())

        withContext(Dispatchers.Main) {
            setSponsorBlockSegments(
                downloadWithItems.downloadSponsorBlockSegments.map { it.toSegment() }
            )

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
        val subtitleInfo = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

        val videoSource = videoUri?.let { videoUri ->
            val videoItem = MediaItem.Builder()
                .setUri(videoUri)
                .setMetadata(downloadWithItems)
                .build()

            ProgressiveMediaSource.Factory(FileDataSource.Factory())
                .createMediaSource(videoItem)
        }

        val audioSource = audioUri?.let { audioUri ->
            val audioItem = MediaItem.Builder()
                .setUri(audioUri)
                .setMetadata(downloadWithItems)
                .build()

            ProgressiveMediaSource.Factory(FileDataSource.Factory())
                .createMediaSource(audioItem)
        }

        val subtitleSource = subtitleInfo?.let { subtitleInfo ->
            val subtitle = SubtitleConfiguration.Builder(subtitleInfo.path.toAndroidUri())
                .setMimeType(MimeTypes.APPLICATION_TTML)
                .setLanguage(subtitleInfo.language ?: "en")
                .build()

            SingleSampleMediaSource.Factory(FileDataSource.Factory())
                .createMediaSource(subtitle, C.TIME_UNSET)
        }

        var mediaSource: MediaSource? = null
        listOfNotNull(videoSource, audioSource, subtitleSource).forEach { source ->
            mediaSource =
                if (mediaSource == null) source else MergingMediaSource(mediaSource!!, source)
        }

        if (mediaSource == null || isAudioOnlyPlayer && audioSource == null) {
            stopSelf()
            return
        }

        exoPlayer?.setMediaSource(mediaSource!!)

        trackSelector?.updateParameters {
            setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
            setPreferredTextLanguage(subtitleInfo?.language ?: "en")
        }
    }

    private suspend fun fillQueue() {
        if (downloadTab == DownloadTab.PLAYLIST) {
            var videos = withContext(Dispatchers.IO) {
                Database.downloadDao().getDownloadPlaylistById(playlistId!!)
            }.downloadVideos

            if (shuffle) videos = listOf(videos.first { it.videoId == videoId }) +
                    videos.filter { it.videoId != videoId }.shuffled()
            else if (downloadSortOrder != null) videos =
                sortDownloadList(videos, downloadSortOrder!!)
            PlayingQueue.setStreams(videos.map { it.toStreamItem() })
        } else {
            var downloads = withContext(Dispatchers.IO) {
                Database.downloadDao().getAll()
            }
                .filterByTab(downloadTab)
                .map { it.download }

            if (shuffle) downloads = downloads.shuffled()
            else if (downloadSortOrder != null) downloads = sortDownloadList(downloads, downloadSortOrder!!)

            PlayingQueue.add(*downloads.map { it.toStreamItem() }.toTypedArray())
        }
    }

    private fun playNextVideo(videoId: String? = null) {
        if (PlayingQueue.repeatMode == Player.REPEAT_MODE_ONE) {
            exoPlayer?.seekTo(0)
        } else if (PlayerHelper.isAutoPlayEnabled() && shouldHandleAutoplay) {
            val nextId = videoId ?: PlayingQueue.getNext() ?: return
            navigateVideo(nextId)
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
