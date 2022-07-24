package com.github.libretube.util

import android.content.Context
import android.view.accessibility.CaptioningManager
import com.github.libretube.obj.PipedStream
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.google.android.exoplayer2.ui.CaptionStyleCompat

object PlayerHelper {
    private val TAG = "PlayerHelper"

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
}
