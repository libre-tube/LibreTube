package com.github.libretube.extensions

import android.net.Uri
import androidx.core.net.toUri
import java.nio.file.Path
import kotlin.io.path.exists

fun Path.toAndroidUri(): Uri? {
    @Suppress("NewApi") // The Path class is desugared.
    return if (exists()) toFile().toUri() else null
}
