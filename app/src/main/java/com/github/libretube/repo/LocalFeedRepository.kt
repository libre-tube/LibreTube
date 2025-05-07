package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.toStreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.parallelMap
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.feed.FeedInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem.ContentAvailability
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

    override suspend fun submitFeedItemChange(feedItem: SubscriptionsFeedItem) {
        DatabaseHolder.Database.feedDao().update(feedItem)
    }

    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        val nowMillis = Instant.now().toEpochMilli()
        val minimumDateMillis = nowMillis - Duration.ofDays(MAX_FEED_AGE_DAYS).toMillis()

        val channelIds = SubscriptionHelper.getSubscriptionChannelIds()
        // remove videos from channels that are no longer subscribed
        DatabaseHolder.Database.feedDao().deleteAllExcept(
            // TODO: the /channel/ prefix is allowed for compatibility reasons and will be removed in the future
            channelIds + channelIds.map { id -> "/channel/${id}" }
        )

        if (!forceRefresh) {
            val feed = DatabaseHolder.Database.feedDao().getAll()
            val oneDayAgo = nowMillis - Duration.ofDays(1).toMillis()

            // only refresh if feed is empty or last refresh was more than a day ago
            val lastRefreshMillis =
                PreferenceHelper.getLong(PreferenceKeys.LAST_LOCAL_FEED_REFRESH_TIMESTAMP_MILLIS, 0)
            if (feed.isNotEmpty() && lastRefreshMillis > oneDayAgo) {
                return DatabaseHolder.Database.feedDao().getAll()
                    .map(SubscriptionsFeedItem::toStreamItem)
            }
        }

        DatabaseHolder.Database.feedDao().cleanUpOlderThan(minimumDateMillis)
        refreshFeed(channelIds, minimumDateMillis, onProgressUpdate)
        PreferenceHelper.putLong(PreferenceKeys.LAST_LOCAL_FEED_REFRESH_TIMESTAMP_MILLIS, nowMillis)

        return DatabaseHolder.Database.feedDao().getAll().map(SubscriptionsFeedItem::toStreamItem)
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

        for (channelIdChunk in channelIds.chunked(CHUNK_SIZE)) {
            val count = channelExtractionCount.get();
            if (count >= BATCH_SIZE) {
                // add a delay after each BATCH_SIZE amount of fully-fetched channels
                delay(CHANNEL_BATCH_DELAY.random())
                channelExtractionCount.set(0)
            }

            val collectedFeedItems = channelIdChunk.parallelMap { channelId ->
                try {
                    getRelatedStreams(channelId, minimumDateMillis).also {
                        if (it.isNotEmpty())
                            // increase counter if we had to fully fetch the channel
                            channelExtractionCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    Log.e(channelId, e.stackTraceToString())
                    null
                } finally {
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(FeedProgress(totalExtractionCount.incrementAndGet(), channelIds.size))
                    }
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
        val feedInfoItems = feedInfo.relatedItems.associateBy { it.url }

        val mostRecentChannelVideo = feedInfo.relatedItems.maxBy {
            it.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0
        } ?: return emptyList()

        // check if the channel has at least one video whose upload time is newer than the maximum
        // feed ago and which is not yet stored in the database
        val mostRecentUploadTime =
            mostRecentChannelVideo.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0
        val hasNewerUploads =
            mostRecentUploadTime > minimumDateMillis && !DatabaseHolder.Database.feedDao()
                .contains(mostRecentChannelVideo.url.toID())
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
            .filter { it.contentAvailability == ContentAvailability.AVAILABLE || it.contentAvailability == ContentAvailability.UPCOMING }

        val channelAvatar = channelInfo.avatars.maxByOrNull { it.height }?.url
        return related.map { item ->
            // avatar is not always included in these info items, thus must be taken from channel info response
            item.toStreamItem(
                channelAvatar,
                // shorts fetched via the shorts tab don't have upload dates so we fall back to the feedInfo
                feedInfoItems[item.url]
            )
        }.filter { it.uploaded > minimumDateMillis }
    }

    companion object {
        /**
         * Amount of feeds that are fetched concurrently.
         *
         * Should ideally be a factor of `BATCH_SIZE` to be correctly applied.
         */
        private const val CHUNK_SIZE = 5

        /**
         * Maximum amount of feeds that should be fetched together, before a delay should be applied.
         */
        private const val BATCH_SIZE = 50

        /**
         * Millisecond delay after fetching `BATCH_SIZE` channels to avoid throttling.
         *
         * A channel is only counted as fetched when it had a recent upload, requiring to fetch
         * the channelInfo via Innertube.
         */
        private val CHANNEL_BATCH_DELAY = (500L..1500L)

        private const val MAX_FEED_AGE_DAYS = 30L // 30 days
    }
}