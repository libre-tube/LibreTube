package com.github.libretube.model;

data class Comment(
    val author: String,
    val thumbnail: String,
    val commentId: String,
    val commentText: String,
    val commentedTime: String,
    val commentorUrl: String,
    val repliesPage: String,
    val likeCount: Int,
    val hearted: Boolean,
    val pinned: Boolean,
    val verified: Boolean,
)
