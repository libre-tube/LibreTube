package com.github.libretube.player.manifest

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi

/**
 * Represents a collection of interchangeable encoded versions of media content.
 */
@OptIn(UnstableApi::class)
data class AdaptationSet
    (
    /** The [C.TrackType] of the media. */
    val trackType: C.TrackType,
    /** [Representation]s of media in the adaption set. */
    val representations: List<Representation>,
)