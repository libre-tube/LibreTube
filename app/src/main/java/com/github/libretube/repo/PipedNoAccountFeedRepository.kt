package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.SubscriptionHelper.GET_SUBSCRIPTIONS_LIMIT
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toID

class PipedNoAccountFeedRepository: FeedRepository {
    override suspend fun getFeed(forceRefresh: Boolean): List<StreamItem> {
        val subscriptions = SubscriptionHelper.getSubscriptions().map { it.url.toID() }

        return when {
            subscriptions.size > GET_SUBSCRIPTIONS_LIMIT ->
                RetrofitInstance.authApi
                    .getUnauthenticatedFeed(subscriptions)

            else -> RetrofitInstance.authApi.getUnauthenticatedFeed(
                subscriptions.joinToString(",")
            )
        }
    }
}