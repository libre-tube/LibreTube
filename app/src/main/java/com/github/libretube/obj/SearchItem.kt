package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchItem(
    var url: String?,
    var thumbnail: String?,
    var uploaderName: String?,
    var uploaded: Long?,
    var shortDescription: String?,
    // Video only attributes
    var title: String?,
    var uploaderUrl: String?,
    var uploaderAvatar: String?,
    var uploadedDate: String?,
    var duration: Long?,
    var views: Long?,
    var uploaderVerified: Boolean?,
    // Channel and Playlist attributes
    var name: String? = null,
    var description: String? = null,
    var subscribers: Long? = -1,
    var videos: Long? = -1,
    var verified: Boolean? = null
) {
    constructor() : this("", "", "", 0, "", "", "", "", "", 0, 0, null)
}
