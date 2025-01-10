package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper.GET_SUBSCRIPTIONS_LIMIT
import com.github.libretube.api.obj.Subscription
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.LocalSubscription

class LocalSubscriptionsRepository: SubscriptionsRepository {
    override suspend fun subscribe(channelId: String) {
        Database.localSubscriptionDao().insert(LocalSubscription(channelId))
    }

    override suspend fun unsubscribe(channelId: String) {
        Database.localSubscriptionDao().delete(LocalSubscription(channelId))
    }

    override suspend fun isSubscribed(channelId: String): Boolean {
        return Database.localSubscriptionDao().includes(channelId)
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        Database.localSubscriptionDao().insertAll(newChannels.map { LocalSubscription(it) })
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        val subscriptions = Database.localSubscriptionDao().getAll().map { it.channelId }

        return when {
            subscriptions.size > GET_SUBSCRIPTIONS_LIMIT ->
                RetrofitInstance.authApi
                    .unauthenticatedSubscriptions(subscriptions)

            else -> RetrofitInstance.authApi.unauthenticatedSubscriptions(
                subscriptions.joinToString(",")
            )
        }
    }
}