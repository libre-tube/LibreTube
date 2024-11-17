package com.github.libretube.api.obj

import android.graphics.drawable.Drawable
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Parcelize
data class ChapterSegment(
    val title: String,
    val image: String = "",
    val start: Long,
    // Used only for video highlights
    @Transient
    @IgnoredOnParcel
    var highlightDrawable: Drawable? = null
): Parcelable {
    companion object {
        /**
         * Length to show for a highlight in seconds
         */
        const val HIGHLIGHT_LENGTH = 10L
    }
}
