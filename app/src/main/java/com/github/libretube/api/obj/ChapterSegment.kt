package com.github.libretube.api.obj

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ChapterSegment(
    val title: String,
    val image: String,
    val start: Long,
    // Used only for video highlights
    @Transient var drawable: Drawable? = null
)
