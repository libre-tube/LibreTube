package com.github.libretube.player.parser

import android.util.Base64
import misc.Common.XTags

class Xtags {
    private var xtags: XTags? = null
    private var enabledFeatures: List<String> = emptyList()

    constructor(xtags: String) {
        this.xtags = XTags.parseFrom(Base64.decode(xtags, Base64.URL_SAFE))
        this.xtags?.xtagsList?.filter { it.value == "1" }?.map { it.key }?.let {
            enabledFeatures = it
        }
    }

    fun isDrcAudio() = enabledFeatures.contains("drc")
}