package com.github.libretube.obj;

import java.util.List;

public class Channel {
    public String id, name, avatarUrl, bannerUrl, description, nextpage;
    public long subscriberCount;
    public boolean verified;
    public List<StreamItem> relatedStreams;

    public Channel(String id, String name, String avatarUrl, String bannerUrl, String description, long subscriberCount,
                   boolean verified, String nextpage, List<StreamItem> relatedStreams) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
        this.description = description;
        this.subscriberCount = subscriberCount;
        this.verified = verified;
        this.nextpage = nextpage;
        this.relatedStreams = relatedStreams;
    }
}
