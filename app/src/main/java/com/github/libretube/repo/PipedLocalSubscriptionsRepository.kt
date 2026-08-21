package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper.GET_SUBSCRIPTIONS_LIMIT
import com.github.libretube.api.obj.Subscription
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.LocalSubscription

class PipedLocalSubscriptionsRepository : SubscriptionsRepository {

    private val dao get() = DatabaseHolder.Database.localSubscriptionDao()

    override suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) {
        dao.insert(LocalSubscription(channelId))
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        dao.insertAll(newChannels.map { LocalSubscription(it) })
    }

    override suspend fun isSubscribed(channelId: String): Boolean {
        return dao.includes(channelId)
    }

    override suspend fun unsubscribe(channelId: String) {
        dao.deleteById(channelId)
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        val channelIds = getSubscriptionChannelIds()

        return if (channelIds.size > GET_SUBSCRIPTIONS_LIMIT) {
            RetrofitInstance.authApi.unauthenticatedSubscriptions(channelIds)
        } else {
            RetrofitInstance.authApi.unauthenticatedSubscriptions(channelIds.joinToString(","))
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return dao.getAll().map { it.channelId }
    }
}
