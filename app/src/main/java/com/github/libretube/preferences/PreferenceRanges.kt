package com.github.libretube.preferences

import com.github.libretube.obj.SliderRange

/**
 * Stores the ranges for the [SliderPreference]
 */
object PreferenceRanges {
    val playbackSpeed = SliderRange(
        0.25f,
        6.0f,
        0.25f,
        1.0f
    )
}
