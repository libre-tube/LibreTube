package com.github.libretube.api

import com.github.libretube.api.obj.Subscription
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.repo.FeedProgress
import com.github.libretube.repo.FeedRepository
import com.github.libretube.repo.UserDataRepository
import com.github.libretube.repo.UserDataRepositoryHelper

object SubscriptionHelper {
    @Suppress("DEPRECATION")
    private val userDataRepository: UserDataRepository
        get() = UserDataRepositoryHelper.userDataRepository
    @Suppress("DEPRECATION")
    private val feedRepository: FeedRepository
        get() = UserDataRepositoryHelper.feedRepository

    suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) = userDataRepository.subscribe(channelId, name, uploaderAvatar, verified)

    suspend fun unsubscribe(channelId: String) {
        userDataRepository.unsubscribe(channelId)
        // remove videos from (local) feed
        feedRepository.removeChannel(channelId)
    }
    suspend fun isSubscribed(channelId: String) = userDataRepository.isSubscribed(channelId)
    suspend fun importSubscriptions(newChannels: List<String>) =
        userDataRepository.importSubscriptions(newChannels)

    suspend fun getSubscriptions() =
        userDataRepository.getSubscriptions().sortedBy { it.name.lowercase() }

    suspend fun getSubscriptionChannelIds() = userDataRepository.getSubscriptionChannelIds()
    suspend fun getFeed(forceRefresh: Boolean, onProgressUpdate: (FeedProgress) -> Unit = {}) =
        feedRepository.getFeed(forceRefresh, onProgressUpdate)

    suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) =
        feedRepository.submitFeedItemChange(feedItem)

    suspend fun submitSubscriptionChannelInfosChanged(subscriptions: List<Subscription>) =
        userDataRepository.submitSubscriptionChannelInfosChanged(subscriptions)
}
