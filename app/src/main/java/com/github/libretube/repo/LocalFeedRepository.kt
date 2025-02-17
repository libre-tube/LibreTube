package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.local.toStreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.parallelMap
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.feed.FeedInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class LocalFeedRepository : FeedRepository {
    private val relevantTabs =
        listOf(
            ContentFilter.LIVESTREAMS to ChannelTabs.LIVESTREAMS,
            ContentFilter.VIDEOS to ChannelTabs.VIDEOS,
            ContentFilter.SHORTS to ChannelTabs.SHORTS
        ).mapNotNull { (filter, tab) ->
            if (filter.isEnabled) tab else null
        }.toTypedArray()

    override suspend fun getFeed(forceRefresh: Boolean): List<StreamItem> {
        val nowMillis = Instant.now().toEpochMilli()
        val minimumDateMillis = nowMillis - Duration.ofDays(MAX_FEED_AGE_DAYS).toMillis()

        val channelIds = SubscriptionHelper.getSubscriptionChannelIds()
        // remove videos from channels that are no longer subscribed
        DatabaseHolder.Database.feedDao().deleteAllExcept(channelIds.map { id -> "/channel/${id}" })

        if (!forceRefresh) {
            val feed = DatabaseHolder.Database.feedDao().getAll()
            val oneDayAgo = nowMillis - Duration.ofDays(1).toMillis()

            // only refresh if feed is empty or last refresh was more than a day ago
            val lastRefreshMillis =
                PreferenceHelper.getLong(PreferenceKeys.LAST_FEED_REFRESH_TIMESTAMP_MILLIS, 0)
            if (feed.isNotEmpty() && lastRefreshMillis > oneDayAgo) {
                return DatabaseHolder.Database.feedDao().getAll()
                    .map(SubscriptionsFeedItem::toStreamItem)
            }
        }

        DatabaseHolder.Database.feedDao().cleanUpOlderThan(minimumDateMillis)
        refreshFeed(channelIds, minimumDateMillis)
        PreferenceHelper.putLong(PreferenceKeys.LAST_FEED_REFRESH_TIMESTAMP_MILLIS, nowMillis)

        return DatabaseHolder.Database.feedDao().getAll().map(SubscriptionsFeedItem::toStreamItem)
    }

    private suspend fun refreshFeed(channelIds: List<String>, minimumDateMillis: Long) {
        val extractionCount = AtomicInteger()

        for (channelIdChunk in channelIds.chunked(CHUNK_SIZE)) {
            // add a delay after each BATCH_SIZE amount of visited channels
            val count = extractionCount.get();
            if (count >= BATCH_SIZE) {
                delay(BATCH_DELAY.random())
                extractionCount.set(0)
            }

            val collectedFeedItems = channelIdChunk.parallelMap { channelId ->
                try {
                    getRelatedStreams(channelId, minimumDateMillis)
                } catch (e: Exception) {
                    Log.e(channelId, e.stackTraceToString())
                    null
                } finally {
                    extractionCount.incrementAndGet();
                }
            }.filterNotNull().flatten().map(StreamItem::toFeedItem)

            DatabaseHolder.Database.feedDao().insertAll(collectedFeedItems)
        }
    }

    private suspend fun getRelatedStreams(
        channelId: String,
        minimumDateMillis: Long
    ): List<StreamItem> {
        val channelUrl = "$YOUTUBE_FRONTEND_URL/channel/${channelId}"
        val feedInfo = FeedInfo.getInfo(channelUrl)

        val mostRecentChannelVideo = feedInfo.relatedItems.maxBy {
            it.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0
        } ?: return emptyList()

        // check if the channel has at least one video whose upload time is newer than the maximum
        // feed ago and which is not yet stored in the database
        val mostRecentUploadTime =
            mostRecentChannelVideo.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0
        val hasNewerUploads =
            mostRecentUploadTime > minimumDateMillis && !DatabaseHolder.Database.feedDao()
                .contains(mostRecentChannelVideo.url.replace(YOUTUBE_FRONTEND_URL, "").toID())
        if (!hasNewerUploads) return emptyList()

        val channelInfo = ChannelInfo.getInfo(channelUrl)

        val relevantInfoTabs = channelInfo.tabs.filter { tab ->
            relevantTabs.any { tab.contentFilters.contains(it) }
        }

        val related = relevantInfoTabs.parallelMap { tab ->
            runCatching {
                ChannelTabInfo.getInfo(NewPipeExtractorInstance.extractor, tab).relatedItems
            }.getOrElse { emptyList() }
        }.flatten().filterIsInstance<StreamInfoItem>()

        return related.map { item ->
            // avatar is not always included in these info items, thus must be taken from channel info response
            item.toStreamItem(channelInfo.avatars.maxByOrNull { it.height }?.url)
        }.filter { it.uploaded > minimumDateMillis }
    }

    companion object {
        private const val CHUNK_SIZE = 2
        /**
         * Maximum amount of feeds that should be fetched together, before a delay should be applied.
         */
        private const val BATCH_SIZE = 50
        /**
         * Millisecond delay between two consecutive batches to avoid throttling.
         */
        private val BATCH_DELAY = (500L..1500L)
        private const val MAX_FEED_AGE_DAYS = 30L // 30 days
    }
}