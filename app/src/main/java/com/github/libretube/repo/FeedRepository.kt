package com.github.libretube.repo

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.db.obj.SubscriptionsFeedItem

data class FeedProgress(
    val currentProgress: Int,
    val total: Int
)

interface FeedRepository {
    suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem>
    suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) {}
}