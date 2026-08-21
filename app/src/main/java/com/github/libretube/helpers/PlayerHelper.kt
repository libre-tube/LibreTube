package com.github.libretube.helpers

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Looper
import android.util.Base64
import androidx.annotation.OptIn
import androidx.core.app.RemoteActionCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.enums.SbSkipOptions
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.obj.VideoStats
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

object PlayerHelper {
    const val CONTROL_TYPE = "control_type"
    const val ROLE_FLAG_AUTO_GEN_SUBTITLE = C.ROLE_FLAG_SUPPLEMENTARY
    private const val MINIMUM_BUFFER_DURATION = 1000 * 10
    const val WATCH_POSITION_TIMER_DELAY_MS = 1000L
    const val FAST_FORWARD_SPEED_FACTOR = 2f
    const val MAXIMUM_PLAYBACK_SPEED = 8f
    const val MAX_BUFFER_DELAY = 10 * 60 * 1000L

    // Backward-compatible aliases — prefer accessing via dedicated helpers
    const val SPONSOR_HIGHLIGHT_CATEGORY = SponsorBlockHelper.SPONSOR_HIGHLIGHT_CATEGORY
    val sponsorBlockEnabled get() = SponsorBlockHelper.sponsorBlockEnabled
    val sponsorBlockNotifications get() = SponsorBlockHelper.sponsorBlockNotifications

    val repeatModes = listOf(
        Player.REPEAT_MODE_OFF to R.string.repeat_mode_none,
        Player.REPEAT_MODE_ONE to R.string.repeat_mode_current,
        Player.REPEAT_MODE_ALL to R.string.repeat_mode_all
    )

    fun createDashSource(streams: Streams, context: Context): Uri {
        val manifest = DashHelper.createManifest(streams, DisplayHelper.supportsHdr(context))
        val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)
        return "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
    }

    fun getFullscreenOrientation(isVerticalVideo: Boolean): Int {
        val pref = PreferenceHelper.getString(PreferenceKeys.FULLSCREEN_ORIENTATION, "ratio")
        return when (pref) {
            "ratio" -> if (isVerticalVideo) {
                ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    //region Preference Properties

    val autoFullscreenEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_FULLSCREEN, false)

    val autoFullscreenShortsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_FULLSCREEN_SHORTS, false)

    val relatedStreamsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.RELATED_STREAMS, true)

    val pausePlayerOnScreenOffEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.PAUSE_ON_SCREEN_OFF, false)

    val watchPositionsVideo: Boolean
        get() = PreferenceHelper.getString(PreferenceKeys.WATCH_POSITIONS, "always") in listOf("always", "videos")

    val watchPositionsAudio: Boolean
        get() = PreferenceHelper.getString(PreferenceKeys.WATCH_POSITIONS, "always") in listOf("always", "audio")

    val watchPositionsAny: Boolean
        get() = watchPositionsVideo || watchPositionsAudio

    val watchHistoryEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.WATCH_HISTORY_TOGGLE, true)

    val useSystemCaptionStyle: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.SYSTEM_CAPTION_STYLE, true)

    val useRichCaptionRendering: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.RICH_CAPTION_RENDERING, false)

    private val bufferingGoal: Int
        get() = PreferenceHelper.getString(PreferenceKeys.BUFFERING_GOAL, "50").toInt() * 1000

    val defaultSubtitleCode: String?
        get() {
            val code = PreferenceHelper.getString(PreferenceKeys.DEFAULT_SUBTITLE, "")
            if (code.isEmpty()) return null
            return if (code.contains("-")) code.split("-")[0] else code
        }

    val skipButtonsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.SKIP_BUTTONS, false)

    var autoPlayEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.AUTOPLAY, true)
        set(value) { PreferenceHelper.putBoolean(PreferenceKeys.AUTOPLAY, value) }

    val autoPlayCountdown: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.AUTOPLAY_COUNTDOWN, false)

    val seekIncrement: Long
        get() = PreferenceHelper.getString(PreferenceKeys.SEEK_INCREMENT, "10.0")
            .toFloat().roundToInt().toLong() * 1000

    private val defaultPlaybackSpeed: Float
        get() = PreferenceHelper.getString(PreferenceKeys.PLAYBACK_SPEED, "1")
            .replace("F", "").toFloat()

    val autoInsertRelatedVideos: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.QUEUE_AUTO_INSERT_RELATED, true)

    val swipeGestureEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.PLAYER_SWIPE_CONTROLS, true)

    val fullscreenGesturesEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.FULLSCREEN_GESTURES, false)

    val pinchGestureEnabled: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.PLAYER_PINCH_CONTROL, true)

    val captionsTextSize: Float
        get() = PreferenceHelper.getString(PreferenceKeys.CAPTIONS_SIZE, "18").toFloat()

    val doubleTapToSeek: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.DOUBLE_TAP_TO_SEEK, true)

    val longPressFastForward: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.LONG_PRESS_FAST_FORWARD, false)

    val alternativePiPControls: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.ALTERNATIVE_PIP_CONTROLS, false)

    private val skipSilence: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.SKIP_SILENCE, false)

    val playAutomatically: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.PLAY_AUTOMATICALLY, true)

    val fullLocalMode: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.FULL_LOCAL_MODE, true)

    val localStreamExtraction: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.LOCAL_STREAM_EXTRACTION, true)

    val localRYD: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.LOCAL_RYD, true)

    var repeatMode: Int
        get() = PreferenceHelper.getInt(PreferenceKeys.REPEAT_MODE, Player.REPEAT_MODE_OFF)
        set(value) { PreferenceHelper.putInt(PreferenceKeys.REPEAT_MODE, value) }

    //endregion

    fun isAutoPlayEnabled(isPlaylist: Boolean = false): Boolean {
        return autoPlayEnabled || (isPlaylist && PreferenceHelper
            .getBoolean(PreferenceKeys.AUTOPLAY_PLAYLISTS, false))
    }

    fun getDefaultResolution(context: Context, isFullscreen: Boolean): Int? {
        var prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.DEFAULT_RESOLUTION_MOBILE
        } else {
            PreferenceKeys.DEFAULT_RESOLUTION
        }
        if (!isFullscreen) prefKey += "_no_fullscreen"
        return PreferenceHelper.getString(prefKey, "").replace("p", "").toIntOrNull()
    }

    //region Player Creation

    @OptIn(UnstableApi::class)
    fun createPlayer(context: Context, trackSelector: DefaultTrackSelector): ExoPlayer {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setRenderersFactory(createRendererFactory(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(getLoadControl())
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply { loadPlaybackParams() }
    }

    @OptIn(UnstableApi::class)
    fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                MINIMUM_BUFFER_DURATION,
                max(bufferingGoal, MINIMUM_BUFFER_DURATION),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    @OptIn(UnstableApi::class)
    fun ExoPlayer.loadPlaybackParams(): ExoPlayer {
        skipSilenceEnabled = skipSilence
        playbackParameters = PlaybackParameters(defaultPlaybackSpeed, 1.0f)
        return this
    }

    @OptIn(UnstableApi::class)
    private fun createRendererFactory(context: Context): DefaultRenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun buildTextRenderers(
                context: Context, output: TextOutput, outputLooper: Looper,
                extensionRendererMode: Int, out: ArrayList<Renderer>
            ) {
                super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
                @Suppress("DEPRECATION")
                (out.last() as? TextRenderer)?.experimentalSetLegacyDecodingEnabled(true)
            }
        }
    }

    //endregion

    //region SponsorBlock (delegated)

    fun getSponsorBlockCategories(): MutableMap<String, SbSkipOptions> =
        SponsorBlockHelper.getCategories()

    fun Player.getCurrentSegment(
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>,
    ): Pair<Segment, SbSkipOptions>? =
        SponsorBlockHelper.run { getCurrentSegment(segments, sponsorBlockConfig) }

    //endregion

    //region Caption & Audio Track (delegated)

    fun getCaptionTracks(player: Player) = CaptionHelper.getCaptionTracks(player)
    fun getCurrentPlayedCaptionFormat(player: Player) = CaptionHelper.getCurrentPlayedCaptionFormat(player)
    fun getCurrentVideoFormat(player: Player) = CaptionHelper.getCurrentVideoFormat(player)
    fun getSubtitleRoleFlags(subtitle: com.github.libretube.api.obj.Subtitle?) = CaptionHelper.getSubtitleRoleFlags(subtitle)
    fun getAudioTrackNameFromFormat(context: Context, pair: Pair<String?, @C.RoleFlags Int>) = CaptionHelper.getAudioTrackNameFromFormat(context, pair)
    fun getAudioLanguagesAndRoleFlagsFromTrackGroups(groups: List<Tracks.Group>, keepOnlySelected: Boolean) = CaptionHelper.getAudioLanguagesAndRoleFlags(groups, keepOnlySelected)
    fun haveAudioTrackRoleFlagSet(@C.RoleFlags roleFlags: Int) = CaptionHelper.haveAudioTrackRoleFlagSet(roleFlags)
    fun getFullAudioRoleFlags(roleFlags: Int, acontValue: String) = CaptionHelper.getFullAudioRoleFlags(roleFlags, acontValue)

    //endregion

    //region Chapters

    fun getCurrentChapterIndex(currentPositionMs: Long, chapters: List<ChapterSegment>): Int? {
        val currentPositionSeconds = currentPositionMs / 1000
        return chapters
            .sortedBy { it.start }
            .indexOfLast { currentPositionSeconds >= it.start }
            .takeIf { it >= 0 }
            ?.takeIf { index ->
                val chapter = chapters[index]
                val isWithinMaxHighlightDuration =
                    (currentPositionSeconds - chapter.start) < ChapterSegment.HIGHLIGHT_LENGTH
                chapter.highlightDrawable == null || isWithinMaxHighlightDuration
            }
    }

    //endregion

    //region PiP & Player Actions (delegated)

    fun getPiPModeActions(activity: Activity, isPlaying: Boolean): List<RemoteActionCompat> =
        PipHelper.getPiPModeActions(activity, isPlaying)

    fun handlePlayerAction(player: Player, playerEvent: PlayerEvent): Boolean =
        PipHelper.handlePlayerAction(player, playerEvent)

    fun getPlayPauseActionIcon(player: Player) = when {
        player.isPlaying -> R.drawable.ic_pause
        player.playbackState == Player.STATE_ENDED -> R.drawable.ic_restart
        else -> R.drawable.ic_play
    }

    //endregion

    //region Watch Position & Stats

    fun saveWatchPosition(player: Player, videoId: String) {
        if (player.duration == C.TIME_UNSET || player.currentPosition in listOf(0L, C.TIME_UNSET)) {
            return
        }
        val watchPosition = WatchPosition(videoId, player.currentPosition)
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseHolder.Database.watchPositionDao().insert(watchPosition)
        }
    }

    @OptIn(UnstableApi::class)
    fun getVideoStats(tracks: Tracks, videoId: String): VideoStats {
        val videoStats = VideoStats(videoId, "", "", "")

        for (group in tracks.groups) {
            if (!group.isSelected || group.length == 0) continue

            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    val audioFormat = (0..group.length).firstOrNull { index ->
                        group.isTrackSelected(index)
                    }?.let { index -> group.getTrackFormat(index) } ?: continue

                    videoStats.audioInfo = "${audioFormat.codecs.orEmpty()} ${
                        TextUtils.formatBitrate(audioFormat.bitrate)
                    }"
                }

                C.TRACK_TYPE_VIDEO -> {
                    val videoFormat = (0..group.length).firstOrNull { index ->
                        group.isTrackSelected(index)
                    }?.let { index -> group.getTrackFormat(index) } ?: continue

                    videoStats.videoInfo = "${videoFormat.codecs.orEmpty()} ${
                        TextUtils.formatBitrate(videoFormat.bitrate)
                    }"
                    videoStats.videoQuality =
                        "${videoFormat.width}x${videoFormat.height} ${videoFormat.frameRate.toInt()}fps"
                }
            }
        }

        return videoStats
    }

    fun isTransientPlayerError(error: PlaybackException): Boolean {
        val errorMsg = error.localizedMessage.orEmpty().lowercase()
        return errorMsg.contains("source error") ||
                errorMsg.contains("ioexception") ||
                errorMsg.contains("network") ||
                errorMsg.contains("timeout") ||
                errorMsg.contains("connection") ||
                errorMsg.contains("sabr") ||
                errorMsg.contains("streaming error") ||
                errorMsg.contains("retrying") ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
    }

    //endregion
}
