package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.obj.Subscription
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.parallelMap
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_DELAY
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_SIZE
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_CHUNK_SIZE
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.channel.ChannelInfo
import java.util.concurrent.atomic.AtomicInteger

class LocalSubscriptionsRepository : SubscriptionsRepository {
    override suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) {
        val localSubscription = LocalSubscription(
            channelId = channelId,
            name = name,
            avatar = uploaderAvatar,
            verified = verified
        )

        Database.localSubscriptionDao().insert(localSubscription)
    }

    override suspend fun unsubscribe(channelId: String) {
        Database.localSubscriptionDao().deleteById(channelId)
    }

    override suspend fun isSubscribed(channelId: String): Boolean {
        return Database.localSubscriptionDao().includes(channelId)
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        val subscribedChannels = getSubscriptionChannelIds()

        val newFiltered = newChannels.filter { !subscribedChannels.contains(it) }

        val failedChannels = mutableListOf<String>()

        val channelExtractionCount = AtomicInteger()
        for (chunk in newFiltered.chunked(CHANNEL_CHUNK_SIZE)) {
            // avoid being rate-limited by adding random delays between requests
            val count = channelExtractionCount.get();
            if (count >= CHANNEL_BATCH_SIZE) {
                // add a delay after each BATCH_SIZE amount of fully-fetched channels
                delay(CHANNEL_BATCH_DELAY.random())
                channelExtractionCount.set(0)
            }

            chunk.parallelMap { channelId ->
                try {
                    val channelUrl = "$YOUTUBE_FRONTEND_URL/channel/${channelId}"
                    val channelInfo = ChannelInfo.getInfo(channelUrl)

                    val avatarUrl = channelInfo.avatars.maxByOrNull { it.height }?.url
                    subscribe(channelId, channelInfo.name, avatarUrl, channelInfo.isVerified)
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    failedChannels.add(channelId)
                }
            }
        }

        if (!failedChannels.isEmpty()) {
            throw Exception("Failed to import ${failedChannels.joinToString(", ")}")
        }
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        // load all channels that have not been fetched yet
        val unfinished = Database.localSubscriptionDao().getChannelsWithoutMetaInfo()
        runCatching {
            importSubscriptions(unfinished.map { it.channelId })
        }

        return Database.localSubscriptionDao().getAll().map {
            Subscription(
                url = it.channelId,
                name = it.name.orEmpty(),
                avatar = it.avatar,
                verified = it.verified
            )
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return Database.localSubscriptionDao().getAll().map { it.channelId }
    }
}