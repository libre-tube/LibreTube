package com.github.libretube.helpers

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.accessibility.CaptioningManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.RemoteActionCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import com.github.libretube.R
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Segment
import com.github.libretube.compat.PendingIntentCompat
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.AudioQuality
import com.github.libretube.enums.PlayerEvent
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object PlayerHelper {
    private const val ACTION_MEDIA_CONTROL = "media_control"
    const val CONTROL_TYPE = "control_type"

    /**
     * Get the audio source following the users preferences
     */
    fun getAudioSource(context: Context, audios: List<PipedStream>): String {
        val audioFormat = PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_FORMAT, "all")
        val audioPrefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.PLAYER_AUDIO_QUALITY_MOBILE
        } else {
            PreferenceKeys.PLAYER_AUDIO_QUALITY
        }

        val audioQuality = PreferenceHelper.getString(audioPrefKey, "best")

        val filteredAudios = audios.filter {
            val audioMimeType = "audio/$audioFormat"
            it.mimeType != audioMimeType || audioFormat == "all"
        }

        return getBitRate(
            filteredAudios,
            if (audioQuality == "best") AudioQuality.BEST else AudioQuality.WORST
        )
    }

    /**
     * Get the best or worst bitrate from a list of audio streams
     * @param audios list of the audio streams
     * @param quality Whether to use the best or worst quality available
     * @return Url of the audio source
     */
    private fun getBitRate(audios: List<PipedStream>, quality: AudioQuality): String {
        val filteredAudios = audios.filter {
            it.bitrate != null
        }.sortedBy {
            it.bitrate
        }
        return when (quality) {
            AudioQuality.BEST -> filteredAudios.last()
            AudioQuality.WORST -> filteredAudios.first()
        }.url!!
    }

    // get the system default caption style
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

    /**
     * get the categories for sponsorBlock
     */
    fun getSponsorBlockCategories(): ArrayList<String> {
        val categories: ArrayList<String> = arrayListOf()
        if (PreferenceHelper.getBoolean(
                "intro_category_key",
                false
            )
        ) {
            categories.add("intro")
        }
        if (PreferenceHelper.getBoolean(
                "selfpromo_category_key",
                false
            )
        ) {
            categories.add("selfpromo")
        }
        if (PreferenceHelper.getBoolean(
                "interaction_category_key",
                false
            )
        ) {
            categories.add("interaction")
        }
        if (PreferenceHelper.getBoolean(
                "sponsors_category_key",
                true
            )
        ) {
            categories.add("sponsor")
        }
        if (PreferenceHelper.getBoolean(
                "outro_category_key",
                false
            )
        ) {
            categories.add("outro")
        }
        if (PreferenceHelper.getBoolean(
                "filler_category_key",
                false
            )
        ) {
            categories.add("filler")
        }
        if (PreferenceHelper.getBoolean(
                "music_offtopic_category_key",
                false
            )
        ) {
            categories.add("music_offtopic")
        }
        if (PreferenceHelper.getBoolean(
                "preview_category_key",
                false
            )
        ) {
            categories.add("preview")
        }
        return categories
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

    val skipSegmentsManually: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.SB_SKIP_MANUALLY,
            false
        )

    val autoPlayEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_PLAY,
            true
        )

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
            true -> PreferenceHelper.getString(PreferenceKeys.BACKGROUND_PLAYBACK_SPEED, "1").toFloat()
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

    fun getDefaultResolution(context: Context): String {
        val prefKey = if (NetworkHelper.isNetworkMetered(context)) {
            PreferenceKeys.DEFAULT_RESOLUTION_MOBILE
        } else {
            PreferenceKeys.DEFAULT_RESOLUTION
        }
        return PreferenceHelper.getString(prefKey, "")
    }

    fun getIntentActon(context: Context): String {
        return context.packageName + "." + ACTION_MEDIA_CONTROL
    }

    private fun getPendingIntent(activity: Activity, code: Int): PendingIntent {
        return PendingIntentCompat.getBroadcast(
            activity,
            code,
            Intent(getIntentActon(activity)).putExtra(CONTROL_TYPE, code),
            0
        )
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
    fun ExoPlayer.loadPlaybackParams(isBackgroundMode: Boolean = false): ExoPlayer {
        skipSilenceEnabled = skipSilence
        val speed = if (isBackgroundMode) backgroundSpeed else playbackSpeed
        playbackParameters = PlaybackParameters(speed, 1.0f)
        return this
    }

    /**
     * Check for SponsorBlock segments matching the current player position
     * @param context A main dispatcher context
     * @param segments List of the SponsorBlock segments
     * @param skipManually Whether the event gets handled by the function caller
     * @return If segment found and [skipManually] is true, the end position of the segment in ms, otherwise null
     */
    fun ExoPlayer.checkForSegments(
        context: Context,
        segments: List<Segment>,
        skipManually: Boolean = false
    ): Long? {
        for (segment in segments) {
            val segmentStart = (segment.segment[0] * 1000f).toLong()
            val segmentEnd = (segment.segment[1] * 1000f).toLong()

            // avoid seeking to the same segment multiple times, e.g. when the SB segment is at the end of the video
            if ((duration - currentPosition).absoluteValue < 500) continue

            if (currentPosition in segmentStart until segmentEnd) {
                if (!skipManually) {
                    if (sponsorBlockNotifications) {
                        runCatching {
                            Toast.makeText(context, R.string.segment_skipped, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    seekTo(segmentEnd)
                } else {
                    return segmentEnd
                }
            }
        }
        return null
    }
}
