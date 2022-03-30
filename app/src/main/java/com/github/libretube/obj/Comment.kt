package com.github.libretube.obj

data class Comment(
    var author: String,
    var thumbnail: String,
    var commentId: String,
    var commentText: String,
    var commentedTime: String,
    var commentorUrl: String,
    var repliesPage: String,
    var likeCount: Int,
    var hearted: Boolean,
    var pinned: Boolean,
    var verified: Boolean
)
