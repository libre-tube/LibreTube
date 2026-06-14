package com.github.libretube.player.manifest

import androidx.media3.common.C.ROLE_FLAG_DESCRIBES_VIDEO
import androidx.media3.common.C.ROLE_FLAG_DUB
import androidx.media3.common.C.ROLE_FLAG_MAIN
import androidx.media3.common.C.ROLE_FLAG_SUPPLEMENTARY
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.player.parser.Xtags
import misc.Common.FormatId

/** A Sabr representation.  */
@UnstableApi
data class Representation(
    /** The format of the representation.  */
    val format: Format,
    /** Metadata about the stream.  */
    val stream: PipedStream,
) {
    fun formatId(): FormatId = FormatId.newBuilder()
        .setItag(stream.itag!!)
        .setLastModified(stream.lastModified!!)
        .setXtags(stream.xtags ?: "")
        .build()

    constructor(stream: PipedStream) : this(
        if (MimeTypes.isVideo(stream.mimeType)) {
            Format.Builder()
                .setCodecs(stream.codec)
                .setContainerMimeType(stream.mimeType)
                .setSampleMimeType(MimeTypes.getVideoMediaMimeType(stream.codec))
                .setAverageBitrate(stream.bitrate ?: -1)
                .setFrameRate(stream.fps?.toFloat() ?: -1f)
                .setWidth(stream.width ?: -1)
                .setHeight(stream.height ?: -1).build()
        } else {
            Format.Builder()
                .setCodecs(stream.codec)
                .setContainerMimeType(stream.mimeType)
                .setSampleMimeType(MimeTypes.getAudioMediaMimeType(stream.codec))
                .setAverageBitrate(stream.bitrate ?: -1)
                .setChannelCount(2)
                .setLanguage(
                    stream.audioTrackId?.take(2) ?: Xtags(stream.xtags.orEmpty()).language()
                    ?: stream.audioTrackLocale
                )
                .setRoleFlags(
                    when (stream.audioTrackType?.lowercase()) {
                        "descriptive" -> ROLE_FLAG_DESCRIBES_VIDEO
                        "original" -> ROLE_FLAG_MAIN
                        "dubbed", "auto-dubbed", "dubbed-auto" -> ROLE_FLAG_DUB
                        "secondary" -> ROLE_FLAG_SUPPLEMENTARY
                        else -> 0
                    }
                )
                .build()
        },
        stream
    )
}
