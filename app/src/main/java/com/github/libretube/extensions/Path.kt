package com.github.libretube.extensions

import android.net.Uri
import androidx.core.net.toUri
import java.nio.file.Path
import kotlin.io.path.exists

fun Path.toAndroidUriOrNull(): Uri? {
    return if (exists()) toAndroidUri() else null
}

fun Path.toAndroidUri(): Uri {
    return toFile().toUri()
}
