package com.github.libretube.player.parser

import android.util.Base64
import misc.Common.XTags

/**
 * Extra tags about a format.
 */
class Xtags {
    private var values: Map<String, String>

    constructor(xtags: String) {
        val xtags = XTags.parseFrom(Base64.decode(xtags, Base64.URL_SAFE))
        values = xtags?.xtagsList?.associate { it.key to it.value }.orEmpty()
    }

    /**
     * Returns whether a feature is enabled.
     */
    private fun isEnabled(feature: String) = values[feature] == "1"

    /**
     *  Whether the format uses [dynamic range compression](https://en.wikipedia.org/wiki/Dynamic_range_compression).
     */
    fun isDrcAudio() = isEnabled("drc")

    /**
     *  The language of the audio track.
     *
     *  For example `en-US`.
     */
    fun language() = values["lang"]

    /**
     *  Whether the audio/voices are artificially boosted.
     */
    fun isVoiceBoosted() = isEnabled("vb")
}