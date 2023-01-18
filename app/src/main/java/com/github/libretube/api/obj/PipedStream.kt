package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class PipedStream(
    var url: String? = null,
    var format: String? = null,
    var quality: String? = null,
    var mimeType: String? = null,
    var codec: String? = null,
    var videoOnly: Boolean? = null,
    var bitrate: Int? = null,
    var initStart: Int? = null,
    var initEnd: Int? = null,
    var indexStart: Int? = null,
    var indexEnd: Int? = null,
    var width: Int? = null,
    var height: Int? = null,
    var fps: Int? = null,
    val audioTrackName: String? = null,
    val audioTrackId: String? = null
)
