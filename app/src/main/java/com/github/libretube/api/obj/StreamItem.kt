package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class StreamItem(
    var url: String? = null,
    val type: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    var uploaderName: String? = null,
    var uploaderUrl: String? = null,
    var uploaderAvatar: String? = null,
    var uploadedDate: String? = null,
    var duration: Long? = null,
    var views: Long? = null,
    var uploaderVerified: Boolean? = null,
    var uploaded: Long? = null,
    var shortDescription: String? = null,
    val isShort: Boolean = false
)
