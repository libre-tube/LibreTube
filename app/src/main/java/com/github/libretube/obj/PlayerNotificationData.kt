package com.github.libretube.obj

import java.nio.file.Path

data class PlayerNotificationData(
    val title: String? = null,
    val uploaderName: String? = null,
    val thumbnailUrl: String? = null,
    val thumbnailPath: Path? = null
)
