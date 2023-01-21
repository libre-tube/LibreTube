package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class StreamItem(
    val url: String? = null,
    val type: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean? = null,
    val uploaded: Long? = null,
    val shortDescription: String? = null,
    val isShort: Boolean = false
)
