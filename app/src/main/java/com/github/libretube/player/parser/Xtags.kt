package com.github.libretube.player.parser

import android.util.Base64
import misc.Common.XTags

/**
 * Extra tags about a format.
 */
class Xtags {
    private var enabledFeatures: List<String> = emptyList()

    constructor(xtags: String) {
        val xtags = XTags.parseFrom(Base64.decode(xtags, Base64.URL_SAFE))
        xtags?.xtagsList?.filter { it.value == "1" }?.map { it.key }?.let {
            enabledFeatures = it
        }
    }

    /**
     *  Whether the format uses [dynamic range compression](https://en.wikipedia.org/wiki/Dynamic_range_compression).
     */
    fun isDrcAudio() = enabledFeatures.contains("drc")

    /**
     *  Whether the audio/voices are artificially boosted.
     */
    fun isVoiceBoosted() = enabledFeatures.contains("vb")
}