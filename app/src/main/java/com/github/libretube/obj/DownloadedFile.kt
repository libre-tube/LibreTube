package com.github.libretube.obj

import com.github.libretube.api.obj.Streams

data class DownloadedFile(
    val name: String,
    val size: Long,
    val type: Int,
    var metadata: Streams? = null
)
