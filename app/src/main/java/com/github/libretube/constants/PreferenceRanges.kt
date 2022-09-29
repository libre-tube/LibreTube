package com.github.libretube.constants

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

    val seekIncrement = SliderRange(
        5f,
        60f,
        5f,
        10f
    )
}
