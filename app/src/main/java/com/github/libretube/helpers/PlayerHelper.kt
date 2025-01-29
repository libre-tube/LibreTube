package com.github.libretube.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.CaptionStyleCompat
import com.github.libretube.LibreTubeApp
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
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.extensions.updateParameters
import com.github.libretube.obj.VideoStats
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

object PlayerHelper {
    private const val ACTION_MEDIA_CONTROL = "media_control"
    const val CONTROL_TYPE = "control_type"
    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"
    const val ROLE_FLAG_AUTO_GEN_SUBTITLE = C.ROLE_FLAG_SUPPLEMENTARY
    private const val MINIMUM_BUFFER_DURATION = 1000 * 10 // exo default is 50s
    const val WATCH_POSITION_TIMER_DELAY_MS = 1000L

    /**
     * The maximum amount of time to wait until the video starts playing: 10 minutes
     */
    const val MAX_BUFFER_DELAY = 10 * 60 * 1000L

    val repeatModes = listOf(
        Player.REPEAT_MODE_OFF to R.string.repeat_mode_none,
        Player.REPEAT_MODE_ONE to R.string.repeat_mode_current,
        Player.REPEAT_MODE_ALL to R.string.repeat_mode_all
    )

    /**
     * A list of all categories that are not disabled by default
     * Also update `sponsorblock_settings.xml` when modifying this!
     */
    private val sbDefaultValues = mapOf(
        "sponsor" to SbSkipOptions.AUTOMATIC,
        "selfpromo" to SbSkipOptions.AUTOMATIC,
        "exclusive_access" to SbSkipOptions.AUTOMATIC,
    )

    /**
     * Create a base64 encoded DASH stream manifest
     */
    fun createDashSource(streams: Streams, context: Context): Uri {
        val manifest = DashHelper.createManifest(
            streams,
            DisplayHelper.supportsHdr(context)
        )

        // encode to base64
        val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)
        return "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
    }

    /**
     * Get the system's default captions style
     */
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getCaptionStyle(context: Context): CaptionStyleCompat {
        val captioningManager = context.getSystemService<CaptioningManager>()!!
        return if (!captioningManager.isEnabled) {
            // system captions are disabled, using android default captions style
            CaptionStyleCompat.DEFAULT
        } else {
            // system captions are enabled
            CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        }
    }

    fun getOrientation(videoWidth: Int, videoHeight: Int): Int {
        val fullscreenOrientationPref = PreferenceHelper.getString(
            PreferenceKeys.FULLSCREEN_ORIENTATION,
            "ratio"
        )

        return when (fullscreenOrientationPref) {
            "ratio" -> {
                // probably a youtube shorts video
                if (videoHeight > videoWidth) {
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                } // a video with normal aspect ratio
                else {
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
        get() = watchPositionsPref in listOf("always", "videos")

    val watchPositionsAudio: Boolean
        get() = watchPositionsPref in listOf("always", "audio")

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
            "50"
        ).toInt() * 1000

    val sponsorBlockEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            "sb_enabled_key",
            true
        )

    private val sponsorBlockNotifications: Boolean
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

            if (code == "") return null

            if (code.contains("-")) {
                return code.split("-")[0]
            }
            return code
        }

    val skipButtonsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SKIP_BUTTONS,
            false
        )

    private val behaviorWhenMinimized
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
            PreferenceHelper.putBoolean(PreferenceKeys.AUTOPLAY, value)
        }

    val autoPlayCountdown: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTOPLAY_COUNTDOWN,
            false
        )

    val seekIncrement: Long
        get() = PreferenceHelper.getString(
            PreferenceKeys.SEEK_INCREMENT,
            "10.0"
        ).toFloat()
            .roundToInt()
            .toLong() * 1000

    private val playbackSpeed: Float
        get() = PreferenceHelper.getString(
            PreferenceKeys.PLAYBACK_SPEED,
            "1"
        ).replace("F", "").toFloat()

    private val backgroundSpeed: Float
        get() = when (PreferenceHelper.getBoolean(PreferenceKeys.CUSTOM_PLAYBACK_SPEED, false)) {
            true -> PreferenceHelper.getString(PreferenceKeys.BACKGROUND_PLAYBACK_SPEED, "1")
                .toFloat()

            else -> playbackSpeed
        }

    val resizeModePref: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.PLAYER_RESIZE_MODE,
            "fit"
        )

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

    private val enabledVideoCodecs: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.ENABLED_VIDEO_CODECS,
            "all"
        )

    private val enabledAudioCodecs: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.ENABLED_AUDIO_CODECS,
            "all"
        )

    val playAutomatically: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PLAY_AUTOMATICALLY,
            true
        )

    val disablePipedProxy: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.DISABLE_VIDEO_IMAGE_PROXY,
            false
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

    val useHlsOverDash: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.USE_HLS_OVER_DASH,
            true
        )

    var repeatMode: Int
        get() = PreferenceHelper.getInt(PreferenceKeys.REPEAT_MODE, Player.REPEAT_MODE_OFF)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.REPEAT_MODE, value)
        }

    fun isAutoPlayEnabled(isPlaylist: Boolean = false): Boolean {
        return autoPlayEnabled || (isPlaylist && PreferenceHelper
            .getBoolean(PreferenceKeys.AUTOPLAY_PLAYLISTS, false))
    }

    private val handleAudioFocus
        get() = !PreferenceHelper.getBoolean(
            PreferenceKeys.ALLOW_PLAYBACK_DURING_CALL,
            false
        )

    fun getDefaultResolution(context: Context, isFullscreen: Boolean): Int? {
        var prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.DEFAULT_RESOLUTION_MOBILE
        } else {
            PreferenceKeys.DEFAULT_RESOLUTION
        }
        if (!isFullscreen) prefKey += "_no_fullscreen"

        return PreferenceHelper.getString(prefKey, "")
            .replace("p", "")
            .toIntOrNull()
    }

    @OptIn(UnstableApi::class)
    fun setPreferredAudioQuality(
        context: Context,
        player: Player,
        trackSelector: DefaultTrackSelector
    ) {
        val prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.PLAYER_AUDIO_QUALITY_MOBILE
        } else {
            PreferenceKeys.PLAYER_AUDIO_QUALITY
        }

        val qualityPref = PreferenceHelper.getString(prefKey, "auto")
        if (qualityPref == "auto") return

        // multiple groups due to different possible audio languages
        val audioTrackGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }

        for (audioTrackGroup in audioTrackGroups) {
            // find the best audio bitrate
            val streams = (0 until audioTrackGroup.length).map { index ->
                index to audioTrackGroup.getTrackFormat(index).bitrate
            }

            // if no bitrate info is available, fallback to the
            // - first stream for lowest quality
            // - last stream for highest quality
            val streamIndex = if (qualityPref == "best") {
                streams.maxByOrNull { it.second }?.takeIf { it.second != -1 }?.first
                    ?: (streams.size - 1)
            } else {
                streams.minByOrNull { it.second }?.takeIf { it.second != -1 }?.first ?: 0
            }

            trackSelector.updateParameters {
                val override = TrackSelectionOverride(audioTrackGroup.mediaTrackGroup, streamIndex)
                setOverrideForType(override)
            }
        }
    }

    fun getIntentActionName(context: Context): String {
        return context.packageName + "." + ACTION_MEDIA_CONTROL
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
        val pendingIntent =
            PendingIntentCompat.getBroadcast(activity, event.ordinal, intent, 0, false)!!

        val text = activity.getString(title)
        val icon = IconCompat.createWithResource(activity, id)

        return RemoteActionCompat(icon, text, text, pendingIntent)
    }

    /**
     * Create controls to use in the PiP window
     */
    fun getPiPModeActions(activity: Activity, isPlaying: Boolean): List<RemoteActionCompat> {
        val audioModeAction = getRemoteAction(
            activity,
            R.drawable.ic_headphones,
            R.string.background_mode,
            PlayerEvent.Background
        )

        val rewindAction = getRemoteAction(
            activity,
            R.drawable.ic_rewind,
            R.string.rewind,
            PlayerEvent.Rewind
        )

        val playPauseAction = getRemoteAction(
            activity,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) R.string.resume else R.string.pause,
            PlayerEvent.PlayPause
        )

        val skipNextAction = getRemoteAction(
            activity,
            R.drawable.ic_next,
            R.string.play_next,
            PlayerEvent.Next
        )

        val forwardAction = getRemoteAction(
            activity,
            R.drawable.ic_forward,
            R.string.forward,
            PlayerEvent.Forward
        )
        return if (alternativePiPControls) {
            listOf(audioModeAction, playPauseAction, skipNextAction)
        } else {
            listOf(rewindAction, playPauseAction, forwardAction)
        }
    }

    /**
     * Create a basic player, that is used for all types of playback situations inside the app
     */
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun createPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector,
        isBackgroundMode: Boolean
    ): ExoPlayer {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setUsePlatformDiagnostics(false)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(getLoadControl())
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .build()
            .apply {
                loadPlaybackParams(isBackgroundMode)
            }
    }

    /**
     * Get the load controls for the player (buffering, etc)
     */
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            // cache the last three minutes
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                MINIMUM_BUFFER_DURATION,
                max(bufferingGoal, MINIMUM_BUFFER_DURATION),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    /**
     * Load playback parameters such as speed and skip silence
     */
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun ExoPlayer.loadPlaybackParams(isBackgroundMode: Boolean = false): ExoPlayer {
        skipSilenceEnabled = skipSilence
        val speed = if (isBackgroundMode) backgroundSpeed else playbackSpeed
        playbackParameters = PlaybackParameters(speed, 1.0f)
        return this
    }

    /**
     * get the categories for sponsorBlock
     */
    fun getSponsorBlockCategories(): MutableMap<String, SbSkipOptions> {
        val categories: MutableMap<String, SbSkipOptions> = mutableMapOf()

        for (category in LibreTubeApp.instance.resources.getStringArray(
            R.array.sponsorBlockSegments
        )) {
            val defaultCategoryValue = sbDefaultValues.getOrDefault(category, SbSkipOptions.OFF)
            val skipOption = PreferenceHelper
                .getString("${category}_category", defaultCategoryValue.name)
                .let { SbSkipOptions.valueOf(it.uppercase()) }

            if (skipOption != SbSkipOptions.OFF) {
                categories[category] = skipOption
            }
        }

        // Add the highlights category to display in the chapters
        if (sponsorBlockHighlights) categories[SPONSOR_HIGHLIGHT_CATEGORY] = SbSkipOptions.OFF
        return categories
    }

    /**
     * Check for SponsorBlock segments matching the current player position
     * @param context A main dispatcher context
     * @param segments List of the SponsorBlock segments
     * @return If segment found and should skip manually, the end position of the segment in ms, otherwise null
     */
    fun Player.checkForSegments(
        context: Context,
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>,
        skipAutomaticallyIfEnabled: Boolean
    ): Segment? {
        for (segment in segments.filter { it.category != SPONSOR_HIGHLIGHT_CATEGORY }) {
            val (start, end) = segment.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()

            // avoid seeking to the same segment multiple times, e.g. when the SB segment is at the end of the video
            if ((duration - currentPosition).absoluteValue < 500) continue
            if (currentPosition !in segmentStart until segmentEnd) continue

            val key = sponsorBlockConfig[segment.category]

            if (!skipAutomaticallyIfEnabled || key == SbSkipOptions.MANUAL ||
                (key == SbSkipOptions.AUTOMATIC_ONCE && segment.skipped)
            ) {
                return segment
            } else if (key == SbSkipOptions.AUTOMATIC ||
                (key == SbSkipOptions.AUTOMATIC_ONCE && !segment.skipped)
            ) {
                if (sponsorBlockNotifications) {
                    runCatching {
                        context.toastFromMainThread(R.string.segment_skipped)
                    }
                }
                seekTo(segmentEnd)
                segment.skipped = true
            } else {
                return null
            }
        }
        return null
    }

    fun Player.isInSegment(segments: List<Segment>): Boolean {
        return segments.any {
            val (start, end) = it.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()
            currentPosition in segmentStart..segmentEnd
        }
    }

    /**
     * Get the name of the currently played chapter
     */
    fun getCurrentChapterIndex(currentPositionMs: Long, chapters: List<ChapterSegment>): Int? {
        val currentPositionSeconds = currentPositionMs / 1000
        return chapters
            .sortedBy { it.start }
            .indexOfLast { currentPositionSeconds >= it.start }
            .takeIf { it >= 0 }
            ?.takeIf { index ->
                val chapter = chapters[index]
                // remove the video highlight if it's already longer ago than [ChapterSegment.HIGHLIGHT_LENGTH],
                // otherwise the SponsorBlock highlight would be shown from its starting point to the end
                val isWithinMaxHighlightDuration =
                    (currentPositionSeconds - chapter.start) < ChapterSegment.HIGHLIGHT_LENGTH
                chapter.highlightDrawable == null || isWithinMaxHighlightDuration
            }
    }

    /**
     * Get the track type string resource corresponding to ExoPlayer role flags used for audio
     * track types.
     *
     * If the role flags doesn't have any role flags used for audio track types, the string
     * resource `unknown_audio_track_type` is returned.
     *
     * @param context   a context to get the string resources used to build the audio track type
     * @param roleFlags the ExoPlayer role flags from which the audio track type will be returned
     * @return the track type string resource corresponding to an ExoPlayer role flag or the
     * `unknown_audio_track_type` one if no role flags corresponding to the ones used for audio
     * track types is set
     */
    private fun getDisplayAudioTrackTypeFromFormat(
        context: Context,
        @C.RoleFlags roleFlags: Int
    ): String {
        // These role flags should not be set together, so the first role only take into account
        // flag which matches
        return when {
            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the descriptive_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_DESCRIBES_VIDEO == C.ROLE_FLAG_DESCRIBES_VIDEO ->
                context.getString(R.string.descriptive_audio_track)

            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the dubbed_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_DUB == C.ROLE_FLAG_DUB ->
                context.getString(R.string.dubbed_audio_track)

            // If the flag ROLE_FLAG_DESCRIBES_VIDEO is set, return the original_or_main_audio_track
            // string resource
            roleFlags and C.ROLE_FLAG_MAIN == C.ROLE_FLAG_MAIN ->
                context.getString(R.string.original_or_main_audio_track)

            // Return the unknown_audio_track_type string resource for any other value
            else -> context.getString(R.string.unknown_audio_track_type)
        }
    }

    /**
     * Get an audio track name from an audio format, using its language tag and its role flags.
     *
     * If the given language is `null`, the string resource `unknown_audio_language` is used
     * instead and when the given role flags have no track type value used by the app, the string
     * resource `unknown_audio_track_type` is used instead.
     *
     * @param context                   a context to get the string resources used to build the
     *                                  audio track name
     * @param audioLanguageAndRoleFlags a pair of an audio format language tag and role flags from
     *                                  which the audio track name will be built
     * @return an audio track name of an audio format language and role flags, localized according
     * to the language preferences of the user
     */
    fun getAudioTrackNameFromFormat(
        context: Context,
        audioLanguageAndRoleFlags: Pair<String?, @C.RoleFlags Int>
    ): String {
        val audioLanguage = audioLanguageAndRoleFlags.first
        return context.getString(R.string.audio_track_format)
            .format(
                if (audioLanguage == null) {
                    context.getString(R.string.unknown_audio_language)
                } else {
                    Locale.forLanguageTag(audioLanguage)
                        .getDisplayLanguage(
                            LocaleHelper.getAppLocale()
                        )
                        .ifEmpty { context.getString(R.string.unknown_audio_language) }
                },
                getDisplayAudioTrackTypeFromFormat(context, audioLanguageAndRoleFlags.second)
            )
    }

    /**
     * Get audio languages with their role flags of supported formats from ExoPlayer track groups
     * and only the selected ones if requested.
     *
     * Duplicate audio languages with their role flags are removed.
     *
     * @param groups                 the list of [Tracks.Group]s of the current tracks played by the player
     * @param keepOnlySelectedTracks whether to get only the selected audio languages with their
     *                               role flags among the supported ones
     * @return a list of distinct audio languages with their role flags from the supported formats
     * of the given track groups and only the selected ones if requested
     */
    fun getAudioLanguagesAndRoleFlagsFromTrackGroups(
        groups: List<Tracks.Group>,
        keepOnlySelectedTracks: Boolean
    ): List<Pair<String?, @C.RoleFlags Int>> {
        // Filter unsupported tracks and keep only selected tracks if requested
        // Use a lambda expression to avoid checking on each audio format if we keep only selected
        // tracks or not
        val trackFilter = if (keepOnlySelectedTracks) {
            { group: Tracks.Group, trackIndex: Int ->
                group.isTrackSupported(trackIndex) && group.isTrackSelected(
                    trackIndex
                )
            }
        } else {
            { group: Tracks.Group, trackIndex: Int -> group.isTrackSupported(trackIndex) }
        }

        return groups.filter {
            it.type == C.TRACK_TYPE_AUDIO
        }.flatMap { group ->
            (0 until group.length).filter {
                trackFilter(group, it)
            }.map { group.getTrackFormat(it) }
        }.map { format ->
            format.language to format.roleFlags
        }.distinct()
    }

    /**
     * Check whether the given flag is set in the given bitfield.
     *
     * @param bitField a bitfield
     * @param flag     a flag to check its presence in the given bitfield
     * @return whether the given flag is set in the given bitfield
     */
    private fun isFlagSet(bitField: Int, flag: Int) = bitField and flag == flag

    /**
     * Check whether the given ExoPlayer role flags contain at least one flag used for audio
     * track types.
     *
     * ExoPlayer role flags currently used for audio track types are [C.ROLE_FLAG_DESCRIBES_VIDEO],
     * [C.ROLE_FLAG_DUB], [C.ROLE_FLAG_MAIN] and [C.ROLE_FLAG_ALTERNATE].
     *
     * @param roleFlags the ExoPlayer role flags to check, an int representing a bitfield
     * @return whether the provided ExoPlayer flags contain a flag used for audio track types
     */
    fun haveAudioTrackRoleFlagSet(@C.RoleFlags roleFlags: Int): Boolean {
        return isFlagSet(roleFlags, C.ROLE_FLAG_DESCRIBES_VIDEO) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_DUB) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_MAIN) ||
                isFlagSet(roleFlags, C.ROLE_FLAG_ALTERNATE)
    }

    @OptIn(androidx.media3.common.util.UnstableApi::class)
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

    fun getPlayPauseActionIcon(player: Player) = when {
        player.isPlaying -> R.drawable.ic_pause
        player.playbackState == Player.STATE_ENDED -> R.drawable.ic_restart
        else -> R.drawable.ic_play
    }

    fun saveWatchPosition(player: Player, videoId: String) {
        if (player.duration == C.TIME_UNSET || player.currentPosition in listOf(0L, C.TIME_UNSET)) {
            return
        }

        val watchPosition = WatchPosition(videoId, player.currentPosition)
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseHolder.Database.watchPositionDao().insert(watchPosition)
        }
    }

    /**
     * Handle basic [PlayerEvent]'s that can be handled by the player itself without context
     */
    fun handlePlayerAction(player: Player, playerEvent: PlayerEvent): Boolean {
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

    @OptIn(UnstableApi::class)
    fun setPreferredCodecs(trackSelector: DefaultTrackSelector) {
        trackSelector.updateParameters {
            val enabledVideoCodecs = PlayerHelper.enabledVideoCodecs
            if (enabledVideoCodecs != "all") {
                // map the codecs to their corresponding mimetypes
                val mimeType = when (enabledVideoCodecs) {
                    "vp9" -> arrayOf("video/webm", "video/x-vnd.on2.vp9")
                    "avc" -> arrayOf("video/mp4", "video/avc")
                    else -> throw IllegalArgumentException()
                }
                this.setPreferredVideoMimeTypes(*mimeType)
            }
            val enabledAudioCodecs = PlayerHelper.enabledAudioCodecs
            if (enabledAudioCodecs != "all") {
                // map the codecs to their corresponding mimetypes
                val mimeType = when (enabledAudioCodecs) {
                    "opus" -> arrayOf("audio/opus")
                    "mp4" -> arrayOf("audio/mp4a-latm")
                    else -> throw IllegalArgumentException()
                }
                this.setPreferredAudioMimeTypes(*mimeType)
            }
        }
    }
}
