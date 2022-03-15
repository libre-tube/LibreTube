package com.github.libretube.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StreamItem(
    var url: String?,
    var title: String?,
    var thumbnail: String?,
    var uploaderName: String?,
    var uploaderUrl: String?,
    var uploaderAvatar: String?,
    var uploadedDate: String?,
    var duration: Long?,
    var views: Long?,
    var uploaderVerified: Boolean?,
    var uploaded: Long?,
    var shortDescription: String?
)
