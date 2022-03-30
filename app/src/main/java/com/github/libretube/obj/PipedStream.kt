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
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as PipedStream
//
//        if (url != other.url) return false
//        if (format != other.format) return false
//        if (quality != other.quality) return false
//        if (mimeType != other.mimeType) return false
//        if (codec != other.codec) return false
//        if (videoOnly != other.videoOnly) return false
//        if (bitrate != other.bitrate) return false
//        if (initStart != other.initStart) return false
//        if (initEnd != other.initEnd) return false
//        if (indexStart != other.indexStart) return false
//        if (indexEnd != other.indexEnd) return false
//        if (width != other.width) return false
//        if (height != other.height) return false
//        if (fps != other.fps) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = format?.hashCode() ?: 0
//        result = 31 * result + (quality?.hashCode() ?: 0)
//        return result
//    }
}
