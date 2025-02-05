package com.github.libretube.services

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.StreamsExtractor
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.checkForSegments
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.YoutubeHlsPlaylistParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/**
 * Loads the selected videos audio in background mode with a notification area.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
open class OnlinePlayerService : AbstractPlayerService() {
    override val isOfflinePlayer: Boolean = false
    override val isAudioOnlyPlayer: Boolean = true

    // PlaylistId/ChannelId for autoplay
    private var playlistId: String? = null
    private var channelId: String? = null
    private var startTimestamp: Long? = null

    /**
     * The response that gets when called the Api.
     */
    private var streams: Streams? = null

    // SponsorBlock Segment data
    private var sponsorBlockAutoSkip = true
    private var sponsorBlockSegments = listOf<Segment>()
    private var sponsorBlockConfig = PlayerHelper.getSponsorBlockCategories()

    private var autoPlayCountdownEnabled = false

    private val scope = CoroutineScope(Dispatchers.IO)

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (!isTransitioning) playNextVideo()
                }

                Player.STATE_IDLE -> {
                    onDestroy()
                }

                Player.STATE_BUFFERING -> {}
                Player.STATE_READY -> {
                    // save video to watch history when the video starts playing or is being resumed
                    // waiting for the player to be ready since the video can't be claimed to be watched
                    // while it did not yet start actually, but did buffer only so far
                    scope.launch(Dispatchers.IO) {
                        streams?.let { streams ->
                            val watchHistoryItem =
                                streams.toStreamItem(videoId).toWatchHistoryItem(videoId)
                            DatabaseHelper.addToWatchHistory(watchHistoryItem)
                        }
                    }
                }
            }
        }
    }

    override suspend fun onServiceCreated(args: Bundle) {
        val playerData = args.parcelable<PlayerData>(IntentData.playerData)
        if (playerData == null) {
            stopSelf()
            return
        }

        // get the intent arguments
        setVideoId(playerData.videoId)
        playlistId = playerData.playlistId
        channelId = playerData.channelId
        startTimestamp = playerData.timestamp

        if (!playerData.keepQueue) PlayingQueue.clear()

        exoPlayer?.addListener(playerListener)
    }

    override suspend fun startPlayback() {
        super.startPlayback()

        val timestamp = startTimestamp ?: 0L
        startTimestamp = null

        streams = withContext(Dispatchers.IO) {
            try {
                StreamsExtractor.extractStreams(videoId)
            } catch (e: Exception) {
                val errorMessage =
                    StreamsExtractor.getExtractorErrorMessageString(this@OnlinePlayerService, e)
                this@OnlinePlayerService.toastFromMainDispatcher(errorMessage)
                return@withContext null
            }
        } ?: return

        if (PlayingQueue.isEmpty()) {
            PlayingQueue.updateQueue(
                streams!!.toStreamItem(videoId),
                playlistId,
                channelId,
                streams!!.relatedStreams
            )
        } else if (PlayingQueue.isLast() && playlistId == null && channelId == null) {
            PlayingQueue.insertRelatedStreams(streams!!.relatedStreams)
        }

        // save the current stream to the queue
        streams?.toStreamItem(videoId)?.let {
            PlayingQueue.updateCurrent(it)
        }

        withContext(Dispatchers.Main) {
            playAudio(timestamp)
        }
    }

    private fun playAudio(seekToPosition: Long) {
        setStreamSource()

        // seek to the previous position if available
        if (seekToPosition != 0L) {
            exoPlayer?.seekTo(seekToPosition)
        } else if (watchPositionsEnabled) {
            DatabaseHelper.getWatchPositionBlocking(videoId)?.let {
                if (!DatabaseHelper.isVideoWatched(it, streams?.duration)) exoPlayer?.seekTo(it)
            }
        }

        exoPlayer?.apply {
            playWhenReady = PlayerHelper.playAutomatically
            prepare()
        }

        if (PlayerHelper.sponsorBlockEnabled) fetchSponsorBlockSegments()
    }

    /**
     * Plays the next video from the queue
     */
    private fun playNextVideo(nextId: String? = null) {
        if (nextId == null) {
            if (PlayingQueue.repeatMode == Player.REPEAT_MODE_ONE) {
                exoPlayer?.seekTo(0)
                return
            }

            if (!PlayerHelper.isAutoPlayEnabled(playlistId != null) || autoPlayCountdownEnabled) return
        }

        val nextVideo = nextId ?: PlayingQueue.getNext() ?: return

        // play new video on background
        setVideoId(nextVideo)
        this.streams = null
        this.sponsorBlockSegments = emptyList()

        scope.launch {
            startPlayback()
        }
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() = scope.launch(Dispatchers.IO) {
        runCatching {
            if (sponsorBlockConfig.isEmpty()) return@runCatching
            sponsorBlockSegments = RetrofitInstance.api.getSegments(
                videoId,
                JsonHelper.json.encodeToString(sponsorBlockConfig.keys),
                """["skip","mute","full","poi","chapter"]"""
            ).segments

            withContext(Dispatchers.Main) {
                updatePlaylistMetadata {
                    // JSON-encode as work-around for https://github.com/androidx/media/issues/564
                    val segments = JsonHelper.json.encodeToString(sponsorBlockSegments)
                    setExtras(bundleOf(IntentData.segments to segments))
                }

                checkForSegments()
            }
        }
    }


    /**
     * check for SponsorBlock segments
     */
    private fun checkForSegments() {
        handler.postDelayed(this::checkForSegments, 100)

        exoPlayer?.checkForSegments(
            this,
            sponsorBlockSegments,
            sponsorBlockConfig,
            skipAutomaticallyIfEnabled = sponsorBlockAutoSkip
        )
    }

    override fun runPlayerCommand(args: Bundle) {
        super.runPlayerCommand(args)

        if (args.containsKey(PlayerCommand.SET_SB_AUTO_SKIP_ENABLED.name)) {
            sponsorBlockAutoSkip = args.getBoolean(PlayerCommand.SET_SB_AUTO_SKIP_ENABLED.name)
        } else if (args.containsKey(PlayerCommand.SET_AUTOPLAY_COUNTDOWN_ENABLED.name)) {
            autoPlayCountdownEnabled =
                args.getBoolean(PlayerCommand.SET_AUTOPLAY_COUNTDOWN_ENABLED.name)
        }
    }

    /**
     * Sets the [MediaItem] with the [streams] into the [exoPlayer]
     */
    private fun setStreamSource() {
        val streams = streams ?: return

        when {
            // LBRY HLS
            PreferenceHelper.getBoolean(
                PreferenceKeys.LBRY_HLS,
                false
            ) && streams.videoStreams.any {
                it.quality.orEmpty().contains("LBRY HLS")
            } -> {
                val lbryHlsUrl = streams.videoStreams.first {
                    it.quality!!.contains("LBRY HLS")
                }.url!!

                val mediaItem =
                    createMediaItem(lbryHlsUrl.toUri(), MimeTypes.APPLICATION_M3U8, streams)
                exoPlayer?.setMediaItem(mediaItem)
            }
            // DASH
            !PlayerHelper.useHlsOverDash && streams.videoStreams.isNotEmpty() -> {
                // only use the dash manifest generated by YT if either it's a livestream or no other source is available
                val dashUri =
                    if (streams.isLive && streams.dash != null) {
                        ProxyHelper.rewriteUrlUsingProxyPreference(
                            streams.dash
                        ).toUri()
                    } else {
                        // skip LBRY urls when checking whether the stream source is usable
                        PlayerHelper.createDashSource(streams, this)
                    }

                val mediaItem = createMediaItem(dashUri, MimeTypes.APPLICATION_MPD, streams)
                exoPlayer?.setMediaItem(mediaItem)
            }
            // HLS
            streams.hls != null -> {
                val hlsMediaSourceFactory = HlsMediaSource.Factory(DefaultDataSource.Factory(this))
                    .setPlaylistParserFactory(YoutubeHlsPlaylistParser.Factory())

                val mediaItem = createMediaItem(
                    ProxyHelper.rewriteUrlUsingProxyPreference(streams.hls).toUri(),
                    MimeTypes.APPLICATION_M3U8,
                    streams
                )
                val mediaSource = hlsMediaSourceFactory.createMediaSource(mediaItem)

                exoPlayer?.setMediaSource(mediaSource)
                return
            }
            // NO STREAM FOUND
            else -> {
                toastFromMainThread(R.string.unknown_error)
                return
            }
        }
    }

    private fun getSubtitleConfigs(): List<SubtitleConfiguration> = streams?.subtitles?.map {
        val roleFlags = getSubtitleRoleFlags(it)
        SubtitleConfiguration.Builder(it.url!!.toUri())
            .setRoleFlags(roleFlags)
            .setLanguage(it.code)
            .setMimeType(it.mimeType).build()
    }.orEmpty()

    private fun createMediaItem(uri: Uri, mimeType: String, streams: Streams) =
        MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setSubtitleConfigurations(getSubtitleConfigs())
            .setMetadata(streams, videoId)
            .build()
}
