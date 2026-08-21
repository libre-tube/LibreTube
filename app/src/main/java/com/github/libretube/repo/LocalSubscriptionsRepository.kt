package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.obj.Subscription
import com.github.libretube.constants.YouTubeConstants
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.dao.LocalSubscriptionDao
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.parallelMap
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_DELAY
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_SIZE
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_CHUNK_SIZE
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.channel.ChannelInfo
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class LocalSubscriptionsRepository : SubscriptionsRepository {

    private val dao: LocalSubscriptionDao get() = DatabaseHolder.Database.localSubscriptionDao()

    override suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) {
        val localSubscription = LocalSubscription(
            channelId = channelId,
            name = name,
            avatar = uploaderAvatar,
            verified = verified
        )
        dao.insert(localSubscription)
    }

    override suspend fun unsubscribe(channelId: String) {
        dao.deleteById(channelId)
    }

    override suspend fun isSubscribed(channelId: String): Boolean {
        return dao.includes(channelId)
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        val subscribedChannels = getSubscriptionChannelIds().toSet()
        val newFiltered = newChannels.filter { it !in subscribedChannels }

        val failedChannels = CopyOnWriteArrayList<String>()
        val channelExtractionCount = AtomicInteger()

        for (chunk in newFiltered.chunked(CHANNEL_CHUNK_SIZE)) {
            val count = channelExtractionCount.get()
            if (count >= CHANNEL_BATCH_SIZE) {
                delay(CHANNEL_BATCH_DELAY.random())
                channelExtractionCount.set(0)
            }

            chunk.parallelMap { channelId ->
                try {
                    val channelUrl = "${YouTubeConstants.FRONTEND_URL}/channel/$channelId"
                    val channelInfo = ChannelInfo.getInfo(channelUrl)
                    val avatarUrl = channelInfo.avatars.maxByOrNull { it.height }?.url
                    subscribe(channelId, channelInfo.name, avatarUrl, channelInfo.isVerified)
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    failedChannels.add(channelId)
                }
            }
        }

        if (failedChannels.isNotEmpty()) {
            throw Exception("Failed to import ${failedChannels.joinToString(", ")}")
        }
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        val unfinished = dao.getChannelsWithoutMetaInfo()
        runCatching { importSubscriptions(unfinished.map { it.channelId }) }

        return dao.getAll().map {
            Subscription(
                url = it.channelId,
                name = it.name.orEmpty(),
                avatar = it.avatar,
                verified = it.verified
            )
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return dao.getAll().map { it.channelId }
    }

    override suspend fun submitSubscriptionChannelInfosChanged(subscriptions: List<Subscription>) {
        dao.updateAll(subscriptions.map {
            LocalSubscription(it.url, it.name, it.avatar, it.verified)
        })
    }
}
