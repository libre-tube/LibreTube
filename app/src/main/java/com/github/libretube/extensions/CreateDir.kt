package com.github.libretube.extensions

import java.io.File

fun File.createDir() = apply {
    if (!this.exists()) this.mkdirs()
}
