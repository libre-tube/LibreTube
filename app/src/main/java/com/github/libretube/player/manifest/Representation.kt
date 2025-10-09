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
    /** The base URLs of the representation.  */
    @JvmField
    val baseUrls: List<BaseUrl?>,
    segmentBase: SegmentBase.SingleSegmentBase,
    @JvmField
    val itag: Int,
    @JvmField
    val lastModified: Long,
    @JvmField
    val xtags: String? = null,
    val stream: PipedStream,
) {
    /**
     * Returns a [RangedUri] defining the location of the representation's initialization data,
     * or null if no initialization data exists.
     */
    @JvmField
    val initializationUri: RangedUri?

    /**
     * Returns a [RangedUri] defining the location of the representation's segment index, or
     * null if the representation provides an index directly.
     */
    @JvmField
    val indexUri: RangedUri?
    private val segmentIndex: SingleSegmentIndex?

    init {
        Assertions.checkArgument(!baseUrls.isEmpty())
        initializationUri = segmentBase.getInitialization(this)
        this.indexUri = segmentBase.index
        // If we have an index uri then the index is defined externally, and we shouldn't return one
        // directly. If we don't, then we can't do better than an index defining a single segment.
        segmentIndex =
            if (indexUri != null) null else SingleSegmentIndex(RangedUri(null, 0, C.TIME_UNSET))
    }

    val index: SabrSegmentIndex?
        /** Returns an index if the representation provides one directly, or null otherwise.  */
        get() = segmentIndex

    fun formatId(): FormatId? =
        FormatId.newBuilder().setItag(itag).setLastModified(lastModified).setXtags(xtags ?: "").build()
}
