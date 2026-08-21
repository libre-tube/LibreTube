package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.toStreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.YouTubeConstants
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.parallelMap
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.feed.FeedInfo
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class LocalFeedRepository : FeedRepository {

    private val feedDao get() = DatabaseHolder.Database.feedDao()

    private val relevantTabs by lazy {
        listOf(
            ContentFilter.LIVESTREAMS to ChannelTabs.LIVESTREAMS,
            ContentFilter.VIDEOS to ChannelTabs.VIDEOS,
            ContentFilter.SHORTS to ChannelTabs.SHORTS
        ).mapNotNull { (filter, tab) -> if (filter.isEnabled) tab else null }
            .toTypedArray()
    }

    override suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) {
        feedDao.update(feedItem)
    }

    override suspend fun removeChannel(channelId: String) {
        feedDao.delete(channelId)
    }

    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        val nowMillis = Instant.now().toEpochMilli()
        val minimumDateMillis = nowMillis - Duration.ofDays(MAX_FEED_AGE_DAYS).toMillis()

        if (!forceRefresh) {
            val cachedFeed = tryGetCachedFeed(nowMillis)
            if (cachedFeed != null) return cachedFeed
        }

        feedDao.cleanUpOlderThan(minimumDateMillis)
        val channelIds = SubscriptionHelper.getSubscriptionChannelIds()
        refreshFeed(channelIds, minimumDateMillis, onProgressUpdate)
        PreferenceHelper.putLong(PreferenceKeys.LAST_LOCAL_FEED_REFRESH_TIMESTAMP_MILLIS, nowMillis)

        return feedDao.getAll().map(SubscriptionsFeedItem::toStreamItem)
    }

    private suspend fun tryGetCachedFeed(nowMillis: Long): List<StreamItem>? {
        val feed = feedDao.getAll()
        if (feed.isEmpty()) return null

        val lastRefreshMillis =
            PreferenceHelper.getLong(PreferenceKeys.LAST_LOCAL_FEED_REFRESH_TIMESTAMP_MILLIS, 0)
        val oneDayAgo = nowMillis - Duration.ofDays(1).toMillis()
        if (lastRefreshMillis <= oneDayAgo) return null

        return feed.map(SubscriptionsFeedItem::toStreamItem)
    }

    private suspend fun refreshFeed(
        channelIds: List<String>,
        minimumDateMillis: Long,
        onProgressUpdate: (FeedProgress) -> Unit
    ) {
        if (channelIds.isEmpty()) return

        val totalExtractionCount = AtomicInteger()
        val channelExtractionCount = AtomicInteger()
        withContext(Dispatchers.Main) {
            onProgressUpdate(FeedProgress(0, channelIds.size))
        }

        for (channelIdChunk in channelIds.chunked(CHANNEL_CHUNK_SIZE)) {
            val count = channelExtractionCount.get()
            if (count >= CHANNEL_BATCH_SIZE) {
                delay(CHANNEL_BATCH_DELAY.random())
                channelExtractionCount.set(0)
            }

            val (channels, collectedFeedItems) = channelIdChunk.parallelMap { channelId ->
                try {
                    getRelatedStreams(channelId, minimumDateMillis).also {
                        if (it.streamItems.isNotEmpty()) {
                            channelExtractionCount.incrementAndGet()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(channelId, e.stackTraceToString())
                    null
                } finally {
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(FeedProgress(totalExtractionCount.incrementAndGet(), channelIds.size))
                    }
                }
            }.filterNotNull().map { it.subscription to it.streamItems }.unzip()

            SubscriptionHelper.submitSubscriptionChannelInfosChanged(channels.filterNotNull())
            feedDao.insertAll(collectedFeedItems.flatten().map(StreamItem::toFeedItem))
        }
    }

    private suspend fun getRelatedStreams(
        channelId: String,
        minimumDateMillis: Long
    ): ChannelFeedResult {
        val channelUrl = "${YouTubeConstants.FRONTEND_URL}/channel/$channelId"
        val feedInfo = FeedInfo.getInfo(channelUrl)
        val feedInfoItems = feedInfo.relatedItems.associateBy { it.url }

        val mostRecentUploadTime = feedInfo.relatedItems
            .maxByOrNull { it.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0 }
            ?.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0

        val hasNewerUploads = mostRecentUploadTime > minimumDateMillis &&
                !feedDao.contains(feedInfo.relatedItems.maxByOrNull {
                    it.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0
                }?.url?.toID().orEmpty())
        if (!hasNewerUploads) return ChannelFeedResult.EMPTY

        val channelInfo = ChannelInfo.getInfo(channelUrl)
        val channelAvatar = channelInfo.avatars.maxByOrNull { it.height }?.url
        val subscription = Subscription(channelId, channelInfo.name, channelAvatar, channelInfo.isVerified)

        val relevantInfoTabs = channelInfo.tabs.filter { tab ->
            relevantTabs.any { tab.contentFilters.contains(it) }
        }

        val streamItems = relevantInfoTabs.parallelMap { tab ->
            runCatching {
                ChannelTabInfo.getInfo(NewPipeExtractorInstance.extractor, tab).relatedItems
            }.getOrElse { emptyList() }
        }.flatten().filterIsInstance<StreamInfoItem>()
            .filter {
                it.contentAvailability in arrayOf(
                    ContentAvailability.AVAILABLE,
                    ContentAvailability.UPCOMING,
                    ContentAvailability.UNKNOWN
                )
            }.map { item ->
                item.toStreamItem(channelAvatar, feedInfoItems[item.url])
            }.filter { it.uploaded > minimumDateMillis }

        return ChannelFeedResult(subscription, streamItems)
    }

    private data class ChannelFeedResult(
        val subscription: Subscription?,
        val streamItems: List<StreamItem>
    ) {
        companion object {
            val EMPTY = ChannelFeedResult(null, emptyList())
        }
    }

    companion object {
        /** Amount of feeds fetched concurrently. Should be a factor of BATCH_SIZE. */
        const val CHANNEL_CHUNK_SIZE = 5

        /** Maximum feeds fetched together before applying a delay. */
        const val CHANNEL_BATCH_SIZE = 50

        /** Millisecond delay after fetching BATCH_SIZE channels to avoid throttling. */
        val CHANNEL_BATCH_DELAY = (500L..1500L)

        private const val MAX_FEED_AGE_DAYS = 30L
    }
}
