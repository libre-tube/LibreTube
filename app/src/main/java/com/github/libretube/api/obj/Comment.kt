package com.github.libretube.api.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
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
) : Parcelable
