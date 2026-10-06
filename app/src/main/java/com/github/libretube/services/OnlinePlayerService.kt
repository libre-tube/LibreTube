package com.github.libretube.services

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleExtractor
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.getSubtitleRoleFlags
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.player.SabrMediaSource
import com.github.libretube.player.manifest.SabrManifest
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.util.DeArrowUtil
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.YoutubeHlsPlaylistParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException

/**
 * Loads the selected videos audio in background mode with a notification area.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
open class OnlinePlayerService : AbstractPlayerService() {
    override val isOfflinePlayer: Boolean = false

    // PlaylistId/ChannelId for autoplay
    private var playlistId: String? = null
    private var channelId: String? = null
    private var startTimestampSeconds: Long? = null

    /**
     * The response that gets when called the Api.
     */
    private var streams: Streams? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    /*
    Current job that's loading a new video (the value is null if no video is loading at the moment).
     */
    private var fetchVideoInfoJob: Job? = null

    /**
     * The ways the current video can be played, ordered by preference.
     * If the first one fails, playback falls back to the next one.
     */
    private enum class StreamSource { DASH, SABR, HLS }

    private var streamSources = listOf<StreamSource>()
    private var streamSourceIndex = 0

    private var consecutiveFetchFailures = 0

    private val hasFallbackSource get() = streamSourceIndex + 1 < streamSources.size

    /**
     * Whether the player has been switched to the next source, which did not become ready yet.
     * The player can still report a stale idle state in this phase, which must not stop the service.
     */
    private var isSwitchingSource = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (!isTransitioning) playNextVideo(relatedStreams = streams?.relatedStreams)
                }

                Player.STATE_IDLE -> {
                    if (exoPlayer?.playerError != null) {
                        // an error moves the player to idle, in this case we try the next source instead
                        if (hasFallbackSource) return
                    } else if (isSwitchingSource) {
                        return
                    }
                    onDestroy()
                }

                Player.STATE_BUFFERING -> {}

                Player.STATE_READY -> {
                    isSwitchingSource = false
                    // save video to watch history when the video starts playing or is being resumed
                    // waiting for the player to be ready since the video can't be claimed to be watched
                    // while it did not yet start actually, but did buffer only so far
                    if (PlayerHelper.watchHistoryEnabled) {
                        val watchHistoryEntry = streams?.toStreamItem(videoId)
                            ?.toWatchHistoryEntry(exoPlayer?.currentPosition) ?: return

                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                UserDataRepositoryHelper.userDataRepository.addToWatchHistory(watchHistoryEntry)
                            }
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
        isAudioOnlyPlayer = args.getBoolean(IntentData.audioOnly)

        // get the intent arguments
        videoId = playerData.videoId!!
        playlistId = playerData.playlistId
        channelId = playerData.channelId
        startTimestampSeconds = playerData.timestamp

        if (!playerData.keepQueue) PlayingQueue.clear()

        exoPlayer?.addListener(playerListener)
        trackSelector?.updateParameters {
            setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudioOnlyPlayer)
        }
    }

    override suspend fun startPlayback() {
        super.startPlayback()

        val timestampMs = startTimestampSeconds?.times(1000) ?: 0L
        startTimestampSeconds = null

        // stop any previous task for loading video info
        fetchVideoInfoJob?.cancelAndJoin()

        // start loading the video info while keeping a reference to the job
        // so that it can be canceled once a different video is loaded
        fetchVideoInfoJob = scope.launch {
            streams = withContext(Dispatchers.IO) {
                try {
                    getStreamsWithRetry().let {
                        DeArrowUtil.deArrowStreams(it, videoId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.stackTraceToString())
                    toastFromMainDispatcher(e.localizedMessage.orEmpty())
                    return@withContext null
                }
            } ?: run {
                skipToNextVideoAfterFailure()
                return@launch
            }
            consecutiveFetchFailures = 0

            streams?.toStreamItem(videoId)?.let {
                // save the current stream to the queue
                PlayingQueue.updateCurrent(it)

                if (!PlayingQueue.hasNext()) {
                    PlayingQueue.updateQueue(it, playlistId, channelId)
                }

                // update feed item with newer information, e.g. more up-to-date views
                SubscriptionHelper.submitFeedItemChange(it.toFeedItem())
            }

            launch {
                val segments = getSponsorBlockSegments()
                withContext(Dispatchers.Main) { setSponsorBlockSegments(segments) }
            }

            withContext(Dispatchers.Main) {
                streams?.let {
                    streamSources = getStreamSources(it)
                    streamSourceIndex = 0
                    isSwitchingSource = false
                }
                setStreamSource()
                configurePlayer(timestampMs)
            }
        }

        fetchVideoInfoJob?.join()
        fetchVideoInfoJob = null
    }

    /**
     * Fetches the streams, retrying on temporary failures (e.g. a flaky network connection),
     * since a single failure would otherwise leave the player loading forever.
     */
    private suspend fun getStreamsWithRetry(): Streams {
        repeat(STREAMS_FETCH_ATTEMPTS - 1) { attempt ->
            try {
                return MediaServiceRepository.instance.getStreams(videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ContentNotAvailableException) {
                // the video is permanently unavailable (private, removed, age restricted, ...)
                throw e
            } catch (e: Exception) {
                Log.w(TAG(), "failed to fetch streams (attempt ${attempt + 1}): $e")
                delay(STREAMS_FETCH_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return MediaServiceRepository.instance.getStreams(videoId)
    }

    /**
     * The video could not be loaded. Instead of leaving the player loading forever, try the next
     * video of the queue (but only a few times in a row, e.g. if there is no network connection).
     */
    private fun skipToNextVideoAfterFailure() {
        if (consecutiveFetchFailures >= MAX_CONSECUTIVE_FETCH_FAILURES) {
            consecutiveFetchFailures = 0
            return
        }

        val nextVideoId = PlayingQueue.getNext() ?: return
        consecutiveFetchFailures++
        navigateVideo(nextVideoId)
    }

    private fun configurePlayer(seekToPositionMs: Long) {
        // seek to the previous position if available
        if (seekToPositionMs != 0L) {
            exoPlayer?.seekTo(seekToPositionMs)
        } else if (watchPositionsEnabled) {
            scope.launch(Dispatchers.IO) {
                UserDataRepositoryHelper.userDataRepository.getFromWatchHistory(videoId)?.metadata?.positionMillis?.let {
                    if (!DatabaseHelper.isVideoWatched(it, streams?.duration)) {
                        withContext(Dispatchers.Main) { exoPlayer?.seekTo(it) }
                    }
                }
            }
        }

        exoPlayer?.apply {
            // automatically start playback when using the audio player
            playWhenReady = PlayerHelper.playAutomatically || isAudioOnlyPlayer
            prepare()
        }
    }

    private suspend fun getSponsorBlockSegments(): List<Segment> {
        return runCatching {
            MediaServiceRepository.instance.getSegments(
                videoId,
                sponsorBlockConfig.keys.toList(),
                listOf("skip", "mute", "full", "poi", "chapter")
            ).segments
        }.getOrElse { emptyList() }
    }

    override fun navigateVideo(videoId: String) {
        this.streams = null

        super.navigateVideo(videoId)
    }

    /**
     * Sets the [MediaItem] with the [streams] into the [exoPlayer]
     */
    private fun setStreamSource() {
        val streams = streams ?: return

        val source = streamSources.getOrNull(streamSourceIndex)
        Log.i(TAG(), "playing with source $source (available: $streamSources)")

        when {
            // SABR
            source == StreamSource.SABR -> {
                val sabrMediaSourceFactory = SabrMediaSource.Factory(
                    SabrManifest(videoId, streams)
                )
                val mediaItem = createMediaItem(
                    streams.serverAbrStreamingUrl!!.toUri(),
                    "application/vnd.yt-ump",
                    streams
                )
                val mediaSource = sabrMediaSourceFactory.createMediaSource(mediaItem)
                val mediaSources = listOf<MediaSource>(mediaSource) + streams.subtitles.map {
                    val format = Format.Builder()
                        .setSampleMimeType(it.mimeType)
                        .setLanguage(it.code)
                        .setRoleFlags(getSubtitleRoleFlags(it))
                        .build()
                    val subtitleParserFactory = DefaultSubtitleParserFactory()
                    val extractorsFactory = ExtractorsFactory {
                        arrayOf(
                            SubtitleExtractor(
                                subtitleParserFactory.create(format), format
                            )
                        )
                    }
                    val progressiveMediaSourceFactory = ProgressiveMediaSource.Factory(
                        DefaultDataSource.Factory(this), extractorsFactory
                    ).setLoadOnlySelectedTracks(true)
                    try {
                        // `enableLazyLoadingWithSingleTrack` is private
                        val method =
                            ProgressiveMediaSource.Factory::class.java.getDeclaredMethod(
                                "enableLazyLoadingWithSingleTrack",
                                Int::class.java,
                                Format::class.java
                            )
                        method.isAccessible = true
                        method.invoke(
                            progressiveMediaSourceFactory, SubtitleExtractor.TRACK_ID,
                            format
                                .buildUpon()
                                .setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES)
                                .setCodecs(format.sampleMimeType)
                                .setCueReplacementBehavior(
                                    subtitleParserFactory.getCueReplacementBehavior(
                                        format
                                    )
                                )
                                .build()
                        )
                    } catch (e: Exception) {
                        Log.w(
                            this::class.simpleName,
                            "failed to set subtitle lazy-loading: ${e.stackTrace}"
                        )
                    }
                    progressiveMediaSourceFactory.createMediaSource(MediaItem.fromUri(it.url!!))
                }.toList()

                exoPlayer?.setMediaSource(MergingMediaSource(*mediaSources.toTypedArray()))
                return
            }
            // DASH
            source == StreamSource.DASH -> {
                // only use the dash manifest generated by YT if either it's a livestream or no other source is available
                val dashUri =
                    if (streams.isLive && !streams.dash.isNullOrBlank()) {
                            streams.dash.toUri()
                    } else {
                        PlayerHelper.createDashSource(streams.copy(videoStreams = streams.videoStreams.filter {
                            it.url?.startsWith("sabr://") != true
                        }), this)
                    }

                val mediaItem = createMediaItem(dashUri, MimeTypes.APPLICATION_MPD, streams)
                exoPlayer?.setMediaItem(mediaItem)
            }
            // HLS as last fallback
            source == StreamSource.HLS && streams.hls != null -> {
                val hlsMediaSourceFactory = HlsMediaSource.Factory(DefaultDataSource.Factory(this))
                    .setPlaylistParserFactory(YoutubeHlsPlaylistParser.Factory())

                val mediaItem = createMediaItem(
                    streams.hls.toUri(),
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

    /**
     * Returns the available ways to play the [streams], ordered by how reliable they are.
     *
     * Plain DASH is preferred, since every segment is a normal ranged HTTP request. SABR needs one
     * blocking round trip per segment, which is too slow to keep the buffer filled on some networks.
     */
    private fun getStreamSources(streams: Streams) = buildList {
        // livestreams can only be played through YouTube's own manifest, which is sometimes empty
        val hasDashSource = if (streams.isLive) {
            !streams.dash.isNullOrBlank()
        } else {
            streams.videoStreams.any { it.url?.startsWith("sabr://") != true }
        }
        if (hasDashSource) add(StreamSource.DASH)
        // skip SABR for livestreams, as the player impl has no support for it
        if (!streams.isLive && streams.serverAbrStreamingUrl != null && streams.videoPlaybackUstreamerConfig != null) {
            add(StreamSource.SABR)
        }
        if (!streams.hls.isNullOrBlank()) add(StreamSource.HLS)
    }

    override fun onPlaybackError(error: PlaybackException): Boolean {
        if (!hasFallbackSource) return false

        Log.w(TAG(), "source ${streamSources[streamSourceIndex]} failed, trying the next one: $error")
        streamSourceIndex++
        isSwitchingSource = true

        val positionMs = exoPlayer?.currentPosition ?: 0L
        setStreamSource()
        configurePlayer(positionMs)
        return true
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

private const val STREAMS_FETCH_ATTEMPTS = 3
private const val MAX_CONSECUTIVE_FETCH_FAILURES = 3
private const val STREAMS_FETCH_RETRY_DELAY_MS = 700L
