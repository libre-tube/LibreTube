package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
    val author: String? = null,
    val commentId: String? = null,
    val commentText: String? = null,
    val commentedTime: String? = null,
    val commentorUrl: String? = null,
    val repliesPage: String? = null,
    val hearted: Boolean? = null,
    val likeCount: Long? = null,
    val pinned: Boolean? = null,
    val thumbnail: String? = null,
    val verified: Boolean? = null,
    val replyCount: Long? = null
)
