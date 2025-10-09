package com.github.libretube.player.manifest

import androidx.media3.common.C.TrackType
import androidx.media3.common.util.UnstableApi

/** Represents a set of interchangeable encoded versions of a media content component.  */
@UnstableApi
data class AdaptationSet(
    /** The [track type][androidx.media3.common.C.TrackType] of the adaptation set.  */
  @JvmField val type: @TrackType Int,
  /** [Representation]s in the adaptation set.  */
  @JvmField
  val representations: List<Representation?>
)