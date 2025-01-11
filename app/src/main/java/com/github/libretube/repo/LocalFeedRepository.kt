package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionsFeedItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.parallelMap
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.time.Duration
import java.time.Instant

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
            val lastRefresh =
                PreferenceHelper.getLong(PreferenceKeys.LAST_FEED_REFRESH_TIMESTAMP_MILLIS, 0)
            if (feed.isNotEmpty() && lastRefresh > oneDayAgo) {
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
        for (channelIdChunk in channelIds.chunked(CHUNK_SIZE)) {
            val collectedFeedItems = channelIdChunk.parallelMap { channelId ->
                try {
                    getRelatedStreams(channelId)
                } catch (e: Exception) {
                    Log.e(channelId, e.stackTraceToString())
                    null
                }
            }.filterNotNull().flatten().map(StreamItem::toFeedItem)
                .filter { it.uploaded > minimumDateMillis }

            DatabaseHolder.Database.feedDao().insertAll(collectedFeedItems)
        }
    }

    private suspend fun getRelatedStreams(channelId: String): List<StreamItem> {
        val channelInfo = ChannelInfo.getInfo("$YOUTUBE_FRONTEND_URL/channel/${channelId}")
        val relevantInfoTabs = channelInfo.tabs.filter { tab ->
            relevantTabs.any { tab.contentFilters.contains(it) }
        }

        val related = relevantInfoTabs.parallelMap { tab ->
            runCatching {
                ChannelTabInfo.getInfo(NewPipeExtractorInstance.extractor, tab).relatedItems
            }.getOrElse { emptyList() }
        }.flatten().filterIsInstance<StreamInfoItem>()

        return related.map { item ->
            StreamItem(
                type = StreamItem.TYPE_STREAM,
                url = item.url.replace(YOUTUBE_FRONTEND_URL, ""),
                title = item.name,
                uploaded = item.uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: 0,
                uploadedDate = item.uploadDate?.offsetDateTime()?.toLocalDateTime()?.toLocalDate()
                    ?.toString(),
                uploaderName = item.uploaderName,
                uploaderUrl = item.uploaderUrl.replace(YOUTUBE_FRONTEND_URL, ""),
                uploaderAvatar = channelInfo.avatars.maxByOrNull { it.height }?.url,
                thumbnail = item.thumbnails.maxByOrNull { it.height }?.url,
                duration = item.duration,
                views = item.viewCount,
                uploaderVerified = item.isUploaderVerified,
                shortDescription = item.shortDescription,
                isShort = item.isShortFormContent
            )
        }
    }

    companion object {
        private const val CHUNK_SIZE = 2
        private const val MAX_FEED_AGE_DAYS = 30L // 30 days
    }
}