package com.github.libretube.extensions

import com.github.libretube.api.obj.PipedStream

fun PipedStream?.qualityString(fileName: String): String {
    this ?: return ""
    return fileName + "_" + quality?.replace(" ", "_") + "_" + format + "." + mimeType?.split("/")?.last()
}
