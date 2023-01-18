package com.github.libretube.util

import android.app.Activity
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.view.accessibility.CaptioningManager
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.AudioQuality
import com.github.libretube.enums.PlayerEvent
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.video.VideoSize
import kotlin.math.roundToInt

object PlayerHelper {
    private const val ACTION_MEDIA_CONTROL = "media_control"
    const val CONTROL_TYPE = "control_type"

    /**
     * Get the audio source following the users preferences
     */
    fun getAudioSource(
        context: Context,
        audios: List<PipedStream>
    ): String {
        val audioFormat = PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_FORMAT, "all")
        val audioPrefKey = if (
            NetworkHelper.isNetworkMobile(context)
        ) {
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
        val captioningManager =
            context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
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

    fun getOrientation(videoSize: VideoSize): Int {
        val fullscreenOrientationPref = PreferenceHelper.getString(
            PreferenceKeys.FULLSCREEN_ORIENTATION,
            "ratio"
        )

        return when (fullscreenOrientationPref) {
            "ratio" -> {
                // probably a youtube shorts video
                if (videoSize.height > videoSize.width) {
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

    val watchPositionsEnabled: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.WATCH_POSITION_TOGGLE,
            true
        )

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

    val videoFormatPreference: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.PLAYER_VIDEO_FORMAT,
            "webm"
        )

    val bufferingGoal: Int
        get() = PreferenceHelper.getString(
            PreferenceKeys.BUFFERING_GOAL,
            "50"
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

    val seekIncrement: Long
        get() = PreferenceHelper.getString(
            PreferenceKeys.SEEK_INCREMENT,
            "10.0"
        ).toFloat()
            .roundToInt()
            .toLong() * 1000

    val playbackSpeed: String
        get() = PreferenceHelper.getString(
            PreferenceKeys.PLAYBACK_SPEED,
            "1"
        ).replace("F", "")

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

    val alternativePiPControls: Boolean
        get() = PreferenceHelper.getBoolean(
            PreferenceKeys.ALTERNATIVE_PIP_CONTROLS,
            false
        )

    fun getDefaultResolution(context: Context): String {
        return if (NetworkHelper.isNetworkMobile(context)) {
            PreferenceHelper.getString(
                PreferenceKeys.DEFAULT_RESOLUTION_MOBILE,
                ""
            )
        } else {
            PreferenceHelper.getString(
                PreferenceKeys.DEFAULT_RESOLUTION,
                ""
            )
        }
    }

    fun getIntentActon(context: Context): String {
        return context.packageName + "." + ACTION_MEDIA_CONTROL
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPendingIntent(activity: Activity, code: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            activity,
            code,
            Intent(getIntentActon(activity)).putExtra(CONTROL_TYPE, code),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getRemoteAction(
        activity: Activity,
        id: Int,
        @StringRes title: Int,
        event: PlayerEvent
    ): RemoteAction {
        val text = activity.getString(title)
        return RemoteAction(
            Icon.createWithResource(activity, id),
            text,
            text,
            getPendingIntent(activity, event.value)
        )
    }

    /**
     * Create controls to use in the PiP window
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getPiPModeActions(activity: Activity, isPlaying: Boolean, isOfflinePlayer: Boolean = false): ArrayList<RemoteAction> {
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
        return if (
            !isOfflinePlayer && alternativePiPControls
        ) {
            arrayListOf(audioModeAction, playPauseAction, skipNextAction)
        } else {
            arrayListOf(rewindAction, playPauseAction, forwardAction)
        }
    }

    /**
     * Load the captions style according to the users preferences
     */
    fun applyCaptionsStyle(context: Context, subtitleView: SubtitleView?) {
        val captionStyle = getCaptionStyle(context)
        subtitleView?.apply {
            setApplyEmbeddedFontSizes(false)
            setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, captionsTextSize)
            if (!useSystemCaptionStyle) return
            setApplyEmbeddedStyles(captionStyle == CaptionStyleCompat.DEFAULT)
            setStyle(captionStyle)
        }
    }
}
