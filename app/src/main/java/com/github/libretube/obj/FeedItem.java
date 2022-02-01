package com.github.libretube.obj;

public class FeedItem {
    public String url, title, thumbnail, uploaderUrl, uploaderName, uploaderAvatar;

    public long views, duration, uploaded;

    public boolean uploaderVerified;

    public FeedItem(String url, String title, String thumbnail, String uploaderUrl, String uploaderName,
                    String uploaderAvatar, long views, long duration, long uploaded, boolean uploaderVerified) {
        this.url = url;
        this.title = title;
        this.thumbnail = thumbnail;
        this.uploaderUrl = uploaderUrl;
        this.uploaderName = uploaderName;
        this.uploaderAvatar = uploaderAvatar;
        this.views = views;
        this.duration = duration;
        this.uploaded = uploaded;
        this.uploaderVerified = uploaderVerified;
    }
}
