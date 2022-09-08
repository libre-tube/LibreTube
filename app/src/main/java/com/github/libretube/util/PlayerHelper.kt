package com.github.libretube.util

import android.content.Context
import android.view.accessibility.CaptioningManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.PipedStream
import com.google.android.exoplayer2.ui.CaptionStyleCompat

object PlayerHelper {
    // get the audio source following the users preferences
    fun getAudioSource(audios: List<PipedStream>): String {
        val audioFormat = PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_FORMAT, "all")
        val audioQuality = PreferenceHelper.getString(PreferenceKeys.PLAYER_AUDIO_QUALITY, "best")

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
}
