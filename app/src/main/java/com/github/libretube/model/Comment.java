package com.github.libretube.model;

public class Comment {
    public String author, thumbnail, commentId, commentText, commentedTime, commentorUrl, repliesPage;
    public int likeCount;
    public boolean hearted, pinned, verified;

    public Comment(String author, String thumbnail, String commentId, String commentText, String commentedTime,
                   String commentorUrl, String repliesPage, int likeCount, boolean hearted, boolean pinned, boolean verified) {
        this.author = author;
        this.thumbnail = thumbnail;
        this.commentId = commentId;
        this.commentText = commentText;
        this.commentedTime = commentedTime;
        this.commentorUrl = commentorUrl;
        this.repliesPage = repliesPage;
        this.likeCount = likeCount;
        this.hearted = hearted;
        this.pinned = pinned;
        this.verified = verified;
    }
}
