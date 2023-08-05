package com.github.libretube.helpers

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Base64
import android.view.accessibility.CaptioningManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteActionCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.CaptionStyleCompat
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.enums.SbSkipOptions
import com.github.libretube.extensions.updateParameters
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.ChaptersBottomSheet
import com.github.libretube.ui.sheets.ExpandedBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking

object PlayerHelper {
    private const val ACTION_MEDIA_CONTROL = "media_control"
    const val CONTROL_TYPE = "control_type"
    private val SPONSOR_CATEGORIES =
        arrayOf(
            "intro",
            "selfpromo",
            "interaction",
            "sponsor",
            "outro",
            "filler",
            "music_offtopic",
            "preview"
        )
    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"

    /**
     * Create a base64 encoded DASH stream manifest
     */
    fun createDashSource(
        streams: Streams,
        context: Context,
        audioOnly: Boolean = false,
        disableProxy: Boolean
    ): Uri {
        val manifest = DashHelper.createManifest(
            streams,
            DisplayHelper.supportsHdr(context),
            audioOnly,
            disableProxy
        )

        // encode to base64
        val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)
        return "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
    }

    /**
     * Get the system's default captions style
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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

    val autoRotationEnabled: Boolean
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
        get() = watchPositionsPref == "always"

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

    val pipEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PICTURE_IN_PICTURE,
            true
        )

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

    val playbackSpeed: Float
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

    val alternativeVideoLayout: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.ALTERNATIVE_PLAYER_LAYOUT,
            false
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

    val pauseOnQuit: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.PAUSE_ON_QUIT,
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

    val enabledVideoCodecs: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.ENABLED_VIDEO_CODECS,
            "all"
        )

    val playAutomatically: Boolean
        get() = PreferenceHelper
            .getBoolean(
                PreferenceKeys.PLAY_AUTOMATICALLY,
                true
            )

    fun getDefaultResolution(context: Context): String {
        val prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.DEFAULT_RESOLUTION_MOBILE
        } else {
            PreferenceKeys.DEFAULT_RESOLUTION
        }
        return PreferenceHelper.getString(prefKey, "")
    }

    /**
     * Apply the preferred audio quality: auto or worst
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun applyPreferredAudioQuality(context: Context, trackSelector: DefaultTrackSelector) {
        val prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.PLAYER_AUDIO_QUALITY_MOBILE
        } else {
            PreferenceKeys.PLAYER_AUDIO_QUALITY
        }
        when (PreferenceHelper.getString(prefKey, "auto")) {
            "worst" -> trackSelector.updateParameters {
                setMaxAudioBitrate(1)
            }
        }
    }

    fun getIntentActon(context: Context): String {
        return context.packageName + "." + ACTION_MEDIA_CONTROL
    }

    private fun getPendingIntent(activity: Activity, code: Int): PendingIntent {
        val intent = Intent(getIntentActon(activity)).putExtra(CONTROL_TYPE, code)
        return PendingIntentCompat.getBroadcast(activity, code, intent, 0, false)
    }

    private fun getRemoteAction(
        activity: Activity,
        id: Int,
        @StringRes title: Int,
        event: PlayerEvent
    ): RemoteActionCompat {
        val text = activity.getString(title)
        return RemoteActionCompat(
            IconCompat.createWithResource(activity, id),
            text,
            text,
            getPendingIntent(activity, event.value)
        )
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
            R.string.pause,
            if (isPlaying) PlayerEvent.Pause else PlayerEvent.Play
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
     * Get the audio attributes to use for the player
     */
    fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    /**
     * Get the load controls for the player (buffering, etc)
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            // cache the last three minutes
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                1000 * 10, // exo default is 50s
                bufferingGoal,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    /**
     * Load playback parameters such as speed and skip silence
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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

        for (category in SPONSOR_CATEGORIES) {
            val state = PreferenceHelper.getString(category + "_category", "off").uppercase()
            if (SbSkipOptions.valueOf(state) != SbSkipOptions.OFF) {
                categories[category] = SbSkipOptions.valueOf(state)
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
    fun ExoPlayer.checkForSegments(
        context: Context,
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>
    ): Long? {
        for (segment in segments.filter { it.category != SPONSOR_HIGHLIGHT_CATEGORY }) {
            val (start, end) = segment.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()

            // avoid seeking to the same segment multiple times, e.g. when the SB segment is at the end of the video
            if ((duration - currentPosition).absoluteValue < 500) continue

            if (currentPosition in segmentStart until segmentEnd) {
                if (sponsorBlockConfig[segment.category] == SbSkipOptions.AUTOMATIC) {
                    if (sponsorBlockNotifications) {
                        runCatching {
                            Toast.makeText(context, R.string.segment_skipped, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    seekTo(segmentEnd)
                } else if (sponsorBlockConfig[segment.category] == SbSkipOptions.MANUAL) {
                    return segmentEnd
                }
            }
        }
        return null
    }

    fun ExoPlayer.isInSegment(segments: List<Segment>): Boolean {
        return segments.any {
            val (start, end) = it.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()
            currentPosition in segmentStart..segmentEnd
        }
    }

    /**
     * Get the name of the currently played chapter
     */
    fun getCurrentChapterIndex(exoPlayer: ExoPlayer, chapters: List<ChapterSegment>): Int? {
        val currentPosition = exoPlayer.currentPosition / 1000
        return chapters.indexOfLast { currentPosition >= it.start }.takeIf { it >= 0 }
    }

    fun getPosition(videoId: String, duration: Long?): Long? {
        if (duration == null) return null

        runCatching {
            val watchPosition = runBlocking {
                DatabaseHolder.Database.watchPositionDao().findById(videoId)
            }
            if (watchPosition != null && watchPosition.position < duration * 1000 * 0.9) {
                return watchPosition.position
            }
        }
        return null
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
}
