package com.github.libretube.player.manifest

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.player.SabrSegmentIndex
import misc.Common.FormatId

/** A Sabr representation.  */
@UnstableApi
class Representation(
    /** The format of the representation.  */
    @JvmField
    val format: Format,
    @JvmField
    val itag: Int,
    @JvmField
    val lastModified: Long,
    @JvmField
    val xtags: String? = null,
    val stream: PipedStream,
) {
    fun formatId(): FormatId? =
        FormatId.newBuilder().setItag(itag).setLastModified(lastModified).setXtags(xtags ?: "").build()
}
