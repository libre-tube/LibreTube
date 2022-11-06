package com.github.libretube.obj

import android.graphics.Bitmap
import com.github.libretube.api.obj.Streams
import com.github.libretube.enums.DownloadType

data class DownloadedFile(
    val name: String,
    val size: Long,
    val type: DownloadType,
    var metadata: Streams? = null,
    var thumbnail: Bitmap? = null
)
