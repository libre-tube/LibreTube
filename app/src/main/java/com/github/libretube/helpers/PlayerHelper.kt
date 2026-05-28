package com.github.libretube.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Looper
import android.util.Base64
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteActionCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
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
import androidx.media3.ui.CaptionStyleCompat
import com.github.libretube.LibreTubeApp
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
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
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object PlayerHelper {

    private const val ACTION_MEDIA_CONTROL = "media_control"

    const val CONTROL_TYPE = "control_type"
    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"
    const val ROLE_FLAG_AUTO_GEN_SUBTITLE = C.ROLE_FLAG_SUPPLEMENTARY

    private const val MINIMUM_BUFFER_DURATION = 10_000
    private const val BACK_BUFFER_DURATION = 180_000

    const val WATCH_POSITION_TIMER_DELAY_MS = 1000L

    const val FAST_FORWARD_SPEED_FACTOR = 2f
    const val MAXIMUM_PLAYBACK_SPEED = 8f
    const val MAX_BUFFER_DELAY = 10 * 60 * 1000L

    private const val DEFAULT_SEEK_INCREMENT = "10.0"
    private const val DEFAULT_PLAYBACK_SPEED = "1"
    private const val DEFAULT_BUFFERING_GOAL = "50"

    val repeatModes = listOf(
        Player.REPEAT_MODE_OFF to R.string.repeat_mode_none,
        Player.REPEAT_MODE_ONE to R.string.repeat_mode_current,
        Player.REPEAT_MODE_ALL to R.string.repeat_mode_all
    )

    /**
     * SponsorBlock defaults
     */
    private val sbDefaultValues = mapOf(
        "sponsor" to SbSkipOptions.AUTOMATIC,
        "selfpromo" to SbSkipOptions.AUTOMATIC,
        "exclusive_access" to SbSkipOptions.AUTOMATIC,
    )

    /**
     * Create DASH source
     */
    fun createDashSource(streams: Streams, context: Context): Uri {
        val manifest = DashHelper.createManifest(
            streams,
            DisplayHelper.supportsHdr(context)
        )

        val encoded = Base64.encodeToString(
            manifest.toByteArray(),
            Base64.NO_WRAP
        )

        return "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
    }

    /**
     * System caption style
     */
    @OptIn(UnstableApi::class)
    fun getCaptionStyle(context: Context): CaptionStyleCompat {
        val captioningManager = context.getSystemService<CaptioningManager>()
            ?: return CaptionStyleCompat.DEFAULT

        return if (!captioningManager.isEnabled) {
            CaptionStyleCompat.DEFAULT
        } else {
            CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        }
    }

    fun getFullscreenOrientation(isVerticalVideo: Boolean): Int {
        return when (
            PreferenceHelper.getString(
                PreferenceKeys.FULLSCREEN_ORIENTATION,
                "ratio"
            )
        ) {
            "ratio" -> {
                if (isVerticalVideo) {
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    val autoFullscreenEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_FULLSCREEN,
            false
        )

    val autoFullscreenShortsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_FULLSCREEN_SHORTS,
            false
        )

    val relatedStreamsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.RELATED_STREAMS,
            true
        )

    val pausePlayerOnScreenOffEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PAUSE_ON_SCREEN_OFF,
            false
        )

    private val watchPositionsPref: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.WATCH_POSITIONS,
            "always"
        )

    val watchPositionsVideo: Boolean
        get() = watchPositionsPref == "always" || watchPositionsPref == "videos"

    val watchPositionsAudio: Boolean
        get() = watchPositionsPref == "always" || watchPositionsPref == "audio"

    val watchPositionsAny: Boolean
        get() = watchPositionsVideo || watchPositionsAudio

    val watchHistoryEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.WATCH_HISTORY_TOGGLE,
            true
        )

    val useSystemCaptionStyle: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SYSTEM_CAPTION_STYLE,
            true
        )

    val useRichCaptionRendering: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.RICH_CAPTION_RENDERING,
            false
        )

    private val bufferingGoal: Int
        get() = PreferenceHelper.getString(
            PreferenceKeys.BUFFERING_GOAL,
            DEFAULT_BUFFERING_GOAL
        ).toInt() * 1000

    val sponsorBlockEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            "sb_enabled_key",
            true
        )

    val sponsorBlockNotifications: Boolean
        get() = PreferenceHelper.getBoolean(
            "sb_notifications_key",
            true
        )

    private val sponsorBlockHighlights: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SB_HIGHLIGHTS,
            true
        )

    val defaultSubtitleCode: String?
        get() {
            val code = PreferenceHelper.getString(
                PreferenceKeys.DEFAULT_SUBTITLE,
                ""
            )

            return code
                .takeIf { it.isNotBlank() }
                ?.substringBefore("-")
        }

    val skipButtonsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SKIP_BUTTONS,
            false
        )

    private val behaviorWhenMinimized: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.BEHAVIOR_WHEN_MINIMIZED,
            "pip"
        )

    val pipEnabled: Boolean
        get() = behaviorWhenMinimized == "pip"

    val pauseOnQuit: Boolean
        get() = behaviorWhenMinimized == "pause"

    var autoPlayEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTOPLAY,
            true
        )
        set(value) {
            PreferenceHelper.putBoolean(
                PreferenceKeys.AUTOPLAY,
                value
            )
        }

    val autoPlayCountdown: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTOPLAY_COUNTDOWN,
            false
        )

    val seekIncrement: Long
        get() = PreferenceHelper.getString(
            PreferenceKeys.SEEK_INCREMENT,
            DEFAULT_SEEK_INCREMENT
        ).toFloat()
            .roundToInt()
            .toLong() * 1000

    private val defaultPlaybackSpeed: Float
        get() = PreferenceHelper.getString(
            PreferenceKeys.PLAYBACK_SPEED,
            DEFAULT_PLAYBACK_SPEED
        )
            .replace("F", "")
            .toFloat()

    val autoInsertRelatedVideos: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.QUEUE_AUTO_INSERT_RELATED,
            true
        )

    val swipeGestureEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PLAYER_SWIPE_CONTROLS,
            true
        )

    val fullscreenGesturesEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.FULLSCREEN_GESTURES,
            false
        )

    val pinchGestureEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PLAYER_PINCH_CONTROL,
            true
        )

    val captionsTextSize: Float
        get() = PreferenceHelper.getString(
            PreferenceKeys.CAPTIONS_SIZE,
            "18"
        ).toFloat()

    val doubleTapToSeek: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.DOUBLE_TAP_TO_SEEK,
            true
        )

    val longPressFastForward: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.LONG_PRESS_FAST_FORWARD,
            false
        )

    private val alternativePiPControls: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.ALTERNATIVE_PIP_CONTROLS,
            false
        )

    private val skipSilence: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SKIP_SILENCE,
            false
        )

    val playAutomatically: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PLAY_AUTOMATICALLY,
            true
        )

    val fullLocalMode: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.FULL_LOCAL_MODE,
            true
        )

    val localStreamExtraction: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.LOCAL_STREAM_EXTRACTION,
            true
        )

    val localRYD: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.LOCAL_RYD,
            true
        )

    var repeatMode: Int
        get() = PreferenceHelper.getInt(
            PreferenceKeys.REPEAT_MODE,
            Player.REPEAT_MODE_OFF
        )
        set(value) {
            PreferenceHelper.putInt(
                PreferenceKeys.REPEAT_MODE,
                value
            )
        }

    fun isAutoPlayEnabled(isPlaylist: Boolean = false): Boolean {
        return autoPlayEnabled || (
                isPlaylist &&
                        PreferenceHelper.getBoolean(
                            PreferenceKeys.AUTOPLAY_PLAYLISTS,
                            false
                        )
                )
    }

    private val handleAudioFocus: Boolean
        get() = !PreferenceHelper.getBoolean(
            PreferenceKeys.ALLOW_PLAYBACK_DURING_CALL,
            false
        )

    fun getDefaultResolution(
        context: Context,
        isFullscreen: Boolean
    ): Int? {
        var prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.DEFAULT_RESOLUTION_MOBILE
        } else {
            PreferenceKeys.DEFAULT_RESOLUTION
        }

        if (!isFullscreen) {
            prefKey += "_no_fullscreen"
        }

        return PreferenceHelper.getString(prefKey, "")
            .replace("p", "")
            .toIntOrNull()
    }

    fun getIntentActionName(context: Context): String {
        return "${context.packageName}.$ACTION_MEDIA_CONTROL"
    }

    private fun getRemoteAction(
        activity: Activity,
        id: Int,
        @StringRes title: Int,
        event: PlayerEvent
    ): RemoteActionCompat {

        val intent = Intent(getIntentActionName(activity))
            .setPackage(activity.packageName)
            .putExtra(CONTROL_TYPE, event)

        val pendingIntent = PendingIntentCompat.getBroadcast(
            activity,
            event.ordinal,
            intent,
            0,
            false
        )!!

        val text = activity.getString(title)

        return RemoteActionCompat(
            IconCompat.createWithResource(activity, id),
            text,
            text,
            pendingIntent
        )
    }

    /**
     * PiP controls
     */
    fun getPiPModeActions(
        activity: Activity,
        isPlaying: Boolean
    ): List<RemoteActionCompat> {

        val playPauseAction = getRemoteAction(
            activity,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) R.string.resume else R.string.pause,
            PlayerEvent.PlayPause
        )

        return if (alternativePiPControls) {
            listOf(
                getRemoteAction(
                    activity,
                    R.drawable.ic_headphones,
                    R.string.background_mode,
                    PlayerEvent.Background
                ),
                playPauseAction,
                getRemoteAction(
                    activity,
                    R.drawable.ic_next,
                    R.string.play_next,
                    PlayerEvent.Next
                )
            )
        } else {
            listOf(
                getRemoteAction(
                    activity,
                    R.drawable.ic_rewind,
                    R.string.rewind,
                    PlayerEvent.Rewind
                ),
                playPauseAction,
                getRemoteAction(
                    activity,
                    R.drawable.ic_forward,
                    R.string.forward,
                    PlayerEvent.Forward
                )
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun createRendererFactory(
        context: Context
    ): DefaultRenderersFactory {

        return object : DefaultRenderersFactory(context) {

            override fun buildTextRenderers(
                context: Context,
                output: TextOutput,
                outputLooper: Looper,
                extensionRendererMode: Int,
                out: ArrayList<Renderer>
            ) {
                super.buildTextRenderers(
                    context,
                    output,
                    outputLooper,
                    extensionRendererMode,
                    out
                )

                @Suppress("DEPRECATION")
                (out.lastOrNull() as? TextRenderer)
                    ?.experimentalSetLegacyDecodingEnabled(true)
            }
        }
    }

    /**
     * Create ExoPlayer
     */
    @OptIn(UnstableApi::class)
    fun createPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector
    ): ExoPlayer {

        val dataSourceFactory = DefaultDataSource.Factory(context)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setRenderersFactory(createRendererFactory(context))
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(getLoadControl())
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .build()
            .apply {
                loadPlaybackParams()
            }
    }

    /**
     * Buffer config
     */
    @OptIn(UnstableApi::class)
    fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBackBuffer(BACK_BUFFER_DURATION, true)
            .setBufferDurationsMs(
                MINIMUM_BUFFER_DURATION,
                max(bufferingGoal, MINIMUM_BUFFER_DURATION),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    /**
     * Playback params
     */
    @OptIn(UnstableApi::class)
    fun ExoPlayer.loadPlaybackParams(): ExoPlayer {
        skipSilenceEnabled = skipSilence

        playbackParameters = PlaybackParameters(
            defaultPlaybackSpeed,
            1.0f
        )

        return this
    }

    /**
     * SponsorBlock categories
     */
    fun getSponsorBlockCategories(): MutableMap<String, SbSkipOptions> {

        val categories = mutableMapOf<String, SbSkipOptions>()

        LibreTubeApp.instance.resources
            .getStringArray(R.array.sponsorBlockSegments)
            .forEach { category ->

                val defaultCategoryValue = sbDefaultValues
                    .getOrDefault(category, SbSkipOptions.OFF)

                val skipOption = PreferenceHelper
                    .getString(
                        "${category}_category",
                        defaultCategoryValue.name
                    )
                    .let {
                        SbSkipOptions.valueOf(it.uppercase())
                    }

                if (skipOption != SbSkipOptions.OFF) {
                    categories[category] = skipOption
                }
            }

        if (sponsorBlockHighlights) {
            categories[SPONSOR_HIGHLIGHT_CATEGORY] =
                SbSkipOptions.OFF
        }

        return categories
    }

    /**
     * SponsorBlock current segment
     */
    fun Player.getCurrentSegment(
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>,
    ): Pair<Segment, SbSkipOptions>? {

        for (segment in segments) {

            if (segment.category == SPONSOR_HIGHLIGHT_CATEGORY) {
                continue
            }

            val (start, end) = segment.segmentStartAndEnd

            val segmentStart = (start * 1000f).toLong()
            val segmentEnd = (end * 1000f).toLong()

            if (segmentEnd - currentPosition in 0..1000) {
                continue
            }

            if (currentPosition !in segmentStart until segmentEnd) {
                continue
            }

            val key = sponsorBlockConfig[segment.category]

            if (
                key == SbSkipOptions.AUTOMATIC_ONCE &&
                segment.skipped
            ) {
                continue
            }

            return segment to (key ?: SbSkipOptions.AUTOMATIC)
        }

        return null
    }

    /**
     * Current chapter index
     */
    fun getCurrentChapterIndex(
        currentPositionMs: Long,
        chapters: List<ChapterSegment>
    ): Int? {

        val currentPositionSeconds = currentPositionMs / 1000

        return chapters
            .sortedBy { it.start }
            .indexOfLast {
                currentPositionSeconds >= it.start
            }
            .takeIf { it >= 0 }
            ?.takeIf { index ->

                val chapter = chapters[index]

                val isWithinMaxHighlightDuration =
                    (currentPositionSeconds - chapter.start) <
                            ChapterSegment.HIGHLIGHT_LENGTH

                chapter.highlightDrawable == null ||
                        isWithinMaxHighlightDuration
            }
    }

    private fun getTracksByType(
        player: Player,
        trackType: Int
    ): List<Format> {

        return buildList {
            player.currentTracks.groups.forEach { trackGroup ->

                if (trackGroup.type != trackType) {
                    return@forEach
                }

                repeat(trackGroup.length) { index ->
                    add(trackGroup.getTrackFormat(index))
                }
            }
        }
    }

    fun getCaptionTracks(player: Player): List<Format> {
        return getTracksByType(player, C.TRACK_TYPE_TEXT)
    }

    private fun getCurrentFormatByTrackType(
        player: Player,
        trackType: Int
    ): Format? {

        player.currentTracks.groups.forEach { trackGroup ->

            if (trackGroup.type != trackType) {
                return@forEach
            }

            repeat(trackGroup.length) { index ->
                if (trackGroup.isTrackSelected(index)) {
                    return trackGroup.getTrackFormat(index)
                }
            }
        }

        return null
    }

    fun getCurrentPlayedCaptionFormat(player: Player): Format? {
        return getCurrentFormatByTrackType(
            player,
            C.TRACK_TYPE_TEXT
        )
    }

    fun getCurrentVideoFormat(player: Player): Format? {
        return getCurrentFormatByTrackType(
            player,
            C.TRACK_TYPE_VIDEO
        )
    }

    fun getSubtitleRoleFlags(subtitle: Subtitle?): Int {

        if (subtitle?.code == null) {
            return 0
        }

        return if (subtitle.autoGenerated == true) {
            ROLE_FLAG_AUTO_GEN_SUBTITLE
        } else {
            C.ROLE_FLAG_CAPTION
        }
    }

    private fun getDisplayAudioTrackTypeFromFormat(
        context: Context,
        @C.RoleFlags roleFlags: Int
    ): String {

        return when {

            roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO ==
                    C.ROLE_FLAG_DESCRIBES_VIDEO ->
                context.getString(
                    R.string.descriptive_audio_track
                )

            roleFlags and C.ROLE_FLAG_DUB ==
                    C.ROLE_FLAG_DUB ->
                context.getString(
                    R.string.dubbed_audio_track
                )

            roleFlags and C.ROLE_FLAG_MAIN ==
                    C.ROLE_FLAG_MAIN ->
                context.getString(
                    R.string.original_or_main_audio_track
                )

            else ->
                context.getString(
                    R.string.unknown_audio_track_type
                )
        }
    }

    fun getAudioTrackNameFromFormat(
        context: Context,
        audioLanguageAndRoleFlags: Pair<String?, @C.RoleFlags Int>
    ): String {

        val language = audioLanguageAndRoleFlags.first

        val localizedLanguage = if (language == null) {
            context.getString(R.string.unknown_audio_language)
        } else {
            Locale.forLanguageTag(language)
                .getDisplayLanguage(Locale.getDefault())
                .ifEmpty {
                    context.getString(
                        R.string.unknown_audio_language
                    )
                }
        }

        return context.getString(R.string.audio_track_format)
            .format(
                localizedLanguage,
                getDisplayAudioTrackTypeFromFormat(
                    context,
                    audioLanguageAndRoleFlags.second
                )
            )
    }

    fun getAudioLanguagesAndRoleFlagsFromTrackGroups(
        groups: List<Tracks.Group>,
        keepOnlySelectedTracks: Boolean
    ): List<Pair<String?, @C.RoleFlags Int>> {

        val trackFilter = if (keepOnlySelectedTracks) {
            { group: Tracks.Group, index: Int ->
                group.isTrackSupported(index) &&
                        group.isTrackSelected(index)
            }
        } else {
            { group: Tracks.Group, index: Int ->
                group.isTrackSupported(index)
            }
        }

        return groups
            .asSequence()
            .filter {
                it.type == C.TRACK_TYPE_AUDIO
            }
            .flatMap { group ->
                (0 until group.length)
                    .asSequence()
                    .filter { trackFilter(group, it) }
                    .map { group.getTrackFormat(it) }
            }
            .map {
                it.language to it.roleFlags
            }
            .distinct()
            .toList()
    }

    private fun isFlagSet(
        bitField: Int,
        flag: Int
    ): Boolean {
        return bitField and flag == flag
    }

    fun haveAudioTrackRoleFlagSet(
        @C.RoleFlags roleFlags: Int
    ): Boolean {

        return isFlagSet(
            roleFlags,
            C.ROLE_FLAG_DESCRIBES_VIDEO
        ) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_DUB) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_MAIN) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_ALTERNATE)
    }

    fun getFullAudioRoleFlags(
        roleFlags: Int,
        acontValue: String
    ): Int {

        val acontRoleFlags = when (
            acontValue.lowercase()
        ) {
            "dubbed" -> C.ROLE_FLAG_DUB
            "descriptive" -> C.ROLE_FLAG_DESCRIBES_VIDEO
            "original" -> C.ROLE_FLAG_MAIN
            else -> C.ROLE_FLAG_ALTERNATE
        }

        return roleFlags or acontRoleFlags
    }

    @OptIn(UnstableApi::class)
    fun getVideoStats(
        tracks: Tracks,
        videoId: String
    ): VideoStats {

        val videoStats = VideoStats(
            videoId,
            "",
            "",
            ""
        )

        tracks.groups.forEach { group ->

            if (!group.isSelected || group.length == 0) {
                return@forEach
            }

            when (group.type) {

                C.TRACK_TYPE_AUDIO -> {

                    val audioFormat =
                        (0 until group.length)
                            .firstOrNull {
                                group.isTrackSelected(it)
                            }
                            ?.let(group::getTrackFormat)
                            ?: return@forEach

                    videoStats.audioInfo =
                        "${audioFormat.codecs.orEmpty()} ${
                            TextUtils.formatBitrate(audioFormat.bitrate)
                        }"
                }

                C.TRACK_TYPE_VIDEO -> {

                    val videoFormat =
                        (0 until group.length)
                            .firstOrNull {
                                group.isTrackSelected(it)
                            }
                            ?.let(group::getTrackFormat)
                            ?: return@forEach

                    videoStats.videoInfo =
                        "${videoFormat.codecs.orEmpty()} ${
                            TextUtils.formatBitrate(videoFormat.bitrate)
                        }"

                    videoStats.videoQuality =
                        "${videoFormat.width}x${videoFormat.height} " +
                                "${videoFormat.frameRate.toInt()}fps"
                }
            }
        }

        return videoStats
    }

    fun getPlayPauseActionIcon(player: Player): Int {
        return when {
            player.isPlaying -> R.drawable.ic_pause
            player.playbackState == Player.STATE_ENDED ->
                R.drawable.ic_restart
            else -> R.drawable.ic_play
        }
    }

    fun saveWatchPosition(
        player: Player,
        videoId: String
    ) {

        val currentPosition = player.currentPosition

        if (
            player.duration == C.TIME_UNSET ||
            currentPosition == 0L ||
            currentPosition == C.TIME_UNSET
        ) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            DatabaseHolder.Database
                .watchPositionDao()
                .insert(
                    WatchPosition(
                        videoId,
                        currentPosition
                    )
                )
        }
    }

    /**
     * Basic player actions
     */
    fun handlePlayerAction(
        player: Player,
        playerEvent: PlayerEvent
    ): Boolean {

        return when (playerEvent) {

            PlayerEvent.PlayPause -> {
                player.togglePlayPauseState()
                true
            }

            PlayerEvent.Forward -> {
                player.seekBy(seekIncrement)
                true
            }

            PlayerEvent.Rewind -> {
                player.seekBy(-seekIncrement)
                true
            }

            else -> false
        }
    }
}