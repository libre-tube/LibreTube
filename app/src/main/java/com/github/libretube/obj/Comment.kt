package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
    val author: String?,
    val commentId: String?,
    val commentText: String?,
    val commentedTime: String?,
    val commentorUrl: String?,
    val repliesPage: String?,
    val hearted: Boolean?,
    val likeCount: Int?,
    val pinned: Boolean?,
    val thumbnail: String?,
    val verified: Boolean?
) {
    constructor() : this("", "", "", "", "", "", null, 0, null, "", null)
}
