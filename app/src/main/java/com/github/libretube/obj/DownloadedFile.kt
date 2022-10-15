package com.github.libretube.obj

import android.graphics.Bitmap
import com.github.libretube.api.obj.Streams

data class DownloadedFile(
    val name: String,
    val size: Long,
    val type: Int,
    var metadata: Streams? = null,
    var thumbnail: Bitmap? = null
)
