package com.github.libretube.player.manifest

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.PipedStream
import misc.Common.FormatId

/** A Sabr representation.  */
@UnstableApi
class Representation(
    /** The format of the representation.  */
    val format: Format,
    val itag: Int,
    val lastModified: Long,
    val xtags: String? = null,
    val stream: PipedStream,
) {
    fun formatId(): FormatId? =
        FormatId.newBuilder().setItag(itag).setLastModified(lastModified).setXtags(xtags ?: "").build()
}
