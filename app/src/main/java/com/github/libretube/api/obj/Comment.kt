package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val author: String,
    val commentId: String,
    val commentText: String?,
    val commentedTime: String,
    val commentorUrl: String,
    val repliesPage: String? = null,
    val hearted: Boolean,
    val likeCount: Long,
    val pinned: Boolean,
    val thumbnail: String,
    val verified: Boolean,
    val replyCount: Long
)
