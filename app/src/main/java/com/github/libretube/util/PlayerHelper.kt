package com.github.libretube.util

import android.content.Context
import android.view.accessibility.CaptioningManager
import com.github.libretube.obj.PipedStream
import com.google.android.exoplayer2.ui.CaptionStyleCompat

object PlayerHelper {
    // get the best bit rate from audio streams
    fun getMostBitRate(audios: List<PipedStream>): String {
        var bitrate = 0
        var index = 0
        for ((i, audio) in audios.withIndex()) {
            val q = audio.quality!!.replace(" kbps", "").toInt()
            if (q > bitrate) {
                bitrate = q
                index = i
            }
        }
        return audios[index].url!!
    }

    // get the system default caption style
    fun getCaptionStyle(context: Context): CaptionStyleCompat {
        val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
        return if (!captioningManager.isEnabled) {
            // system captions are disabled, using android default captions style
            CaptionStyleCompat.DEFAULT
        } else {
            // system captions are enabled
            CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        }
    }
}