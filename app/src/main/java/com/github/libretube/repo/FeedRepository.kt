package com.github.libretube.repo

import com.github.libretube.api.obj.StreamItem

interface FeedRepository {
    suspend fun getFeed(forceRefresh: Boolean): List<StreamItem>
}