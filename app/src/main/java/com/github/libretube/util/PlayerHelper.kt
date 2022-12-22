package com.github.libretube.util

import android.content.Context
import android.content.pm.ActivityInfo
import android.view.accessibility.CaptioningManager
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.constants.PreferenceKeys
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.video.VideoSize
import kotlin.math.roundToInt

object PlayerHelper {
    // get the audio source following the users preferences
    fun getAudioSource(
        context: Context,
        audios: List<PipedStream>
    ): String {
        val audioFormat = PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_FORMAT, "all")
        val audioQuality = if (
            NetworkHelper.isNetworkMobile(context)
        ) {
            PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_QUALITY_MOBILE, "best")
        } else {
            PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_QUALITY, "best")
        }

        val mutableAudios = audios.toMutableList()
        if (audioFormat != "all") {
            audios.forEach {
                val audioMimeType = "audio/$audioFormat"
                if (it.mimeType != audioMimeType) mutableAudios.remove(it)
            }
        }

        return if (audioQuality == "worst") {
            getLeastBitRate(mutableAudios)
        } else {
            getMostBitRate(mutableAudios)
        }
    }

    // get the best bit rate from audio streams
    private fun getMostBitRate(audios: List<PipedStream>): String {
        var bitrate = 0
        var audioUrl = ""
        audios.forEach {
            if (it.bitrate != null && it.bitrate!! > bitrate) {
                bitrate = it.bitrate!!
                audioUrl = it.url.toString()
            }
        }
        return audioUrl
    }

    // get the best bit rate from audio streams
    private fun getLeastBitRate(audios: List<PipedStream>): String {
        var bitrate = 1000000000
        var audioUrl = ""
        audios.forEach {
            if (it.bitrate != null && it.bitrate!! < bitrate) {
                bitrate = it.bitrate!!
                audioUrl = it.url.toString()
            }
        }
        return audioUrl
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
}
