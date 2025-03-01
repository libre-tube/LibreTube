package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.SubscriptionHelper.GET_SUBSCRIPTIONS_LIMIT
import com.github.libretube.api.obj.StreamItem

class PipedNoAccountFeedRepository : FeedRepository {
    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        val channelIds = SubscriptionHelper.getSubscriptionChannelIds()

        return when {
            channelIds.size > GET_SUBSCRIPTIONS_LIMIT ->
                RetrofitInstance.authApi
                    .getUnauthenticatedFeed(channelIds)

            else -> RetrofitInstance.authApi.getUnauthenticatedFeed(
                channelIds.joinToString(",")
            )
        }
    }
}