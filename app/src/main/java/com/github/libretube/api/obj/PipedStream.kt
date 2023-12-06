package com.github.libretube.api.obj

import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import kotlin.io.path.Path
import kotlinx.serialization.Serializable

@Serializable
data class PipedStream(
    val url: String? = null,
    val format: String? = null,
    val quality: String? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val videoOnly: Boolean? = null,
    val bitrate: Int? = null,
    val initStart: Int? = null,
    val initEnd: Int? = null,
    val indexStart: Int? = null,
    val indexEnd: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Int? = null,
    val audioTrackName: String? = null,
    val audioTrackId: String? = null,
    val contentLength: Long = -1,
    val audioTrackType: String? = null,
    val audioTrackLocale: String? = null
) {
    private fun getQualityString(fileName: String): String {
        return "${fileName}_${quality?.replace(" ", "_")}_$format." +
            mimeType?.split("/")?.last()
    }

    fun toDownloadItem(fileType: FileType, videoId: String, fileName: String) = DownloadItem(
        type = fileType,
        videoId = videoId,
        fileName = getQualityString(fileName),
        path = Path(""),
        url = url,
        format = format,
        quality = quality,
        language = audioTrackLocale,
        downloadSize = contentLength
    )
}
