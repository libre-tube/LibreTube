package com.github.libretube.player.manifest

import androidx.media3.common.Format

/**
 * A specific encoded version of the media content.
 */
data class Representation(
    val itag: Int,
    val lastModified: Long,
    val xtags: String?,
    val format: Format,
)