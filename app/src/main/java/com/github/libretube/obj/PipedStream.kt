package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PipedStream(
    var url: String?,
    var format: String?,
    var quality: String?,
    var mimeType: String?,
    var codec: String?,
    var videoOnly: Boolean?,
    var bitrate: Int?,
    var initStart: Int?,
    var initEnd: Int?,
    var indexStart: Int?,
    var indexEnd: Int?,
    var width: Int?,
    var height: Int?,
    var fps: Int?
) {
    constructor() : this("", "", "", "", "", null, -1, -1, -1, -1, -1, -1, -1, -1)
}
