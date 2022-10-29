package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContentItem(
    var url: String? = null,
    var thumbnail: String? = null,
    var uploaderName: String? = null,
    var uploaded: Long? = null,
    var shortDescription: String? = null,
    // Video only attributes
    var title: String? = null,
    var uploaderUrl: String? = null,
    var uploaderAvatar: String? = null,
    var uploadedDate: String? = null,
    var duration: Long? = null,
    var views: Long? = null,
    var uploaderVerified: Boolean? = null,
    // Channel and Playlist attributes
    var name: String? = null,
    var description: String? = null,
    var subscribers: Long? = -1,
    var videos: Long? = -1,
    var verified: Boolean? = null
)
