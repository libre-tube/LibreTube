package com.github.libretube.db

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.dao.WatchHistoryRow
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.enums.WatchHistoryStatus
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object DatabaseHelper {
    private const val MAX_SEARCH_HISTORY_SIZE = 20

    // can only mark as watched if less than 60s remaining
    private const val ABSOLUTE_WATCHED_THRESHOLD = 60.0f

    // can only mark as watched if at least 75% watched
    private const val RELATIVE_WATCHED_THRESHOLD = 0.75f

    suspend fun addToWatchHistory(watchHistoryItem: WatchHistoryItem) =
        withContext(Dispatchers.IO) {
            Database.watchHistoryDao().insert(watchHistoryItem)
        }

    data class WatchHistoryPage(val rows: List<WatchHistoryRow>, val nextCursor: Long?) {
        val items get() = rows.map(WatchHistoryRow::item)
    }

    suspend fun getWatchHistoryPage(
        pageSize: Int,
        statusFilter: WatchHistoryStatus = WatchHistoryStatus.ALL,
        cursor: Long = Long.MAX_VALUE
    ): WatchHistoryPage {
        val rows = Database.watchHistoryDao().getPage(
            limit = pageSize,
            cursor = cursor,
            watched = statusFilter.isWatched,
            absoluteWatchedThresholdSeconds = ABSOLUTE_WATCHED_THRESHOLD,
            relativeWatchedThreshold = RELATIVE_WATCHED_THRESHOLD
        )

        return WatchHistoryPage(
            rows = rows,
            nextCursor = rows.lastOrNull()?.rowId?.minus(1)?.takeIf { rows.size == pageSize }
        )
    }

    suspend fun addToSearchHistory(searchHistoryItem: SearchHistoryItem) {
        Database.searchHistoryDao().insert(searchHistoryItem)

        if (PreferenceHelper.getBoolean(PreferenceKeys.UNLIMITED_SEARCH_HISTORY, false)) return

        // delete the first watch history entry if the limit is reached
        val searchHistory = Database.searchHistoryDao().getAll().toMutableList()

        while (searchHistory.size > MAX_SEARCH_HISTORY_SIZE) {
            Database.searchHistoryDao().delete(searchHistory.first())
            searchHistory.removeAt(0)
        }
    }

    suspend fun getWatchPosition(videoId: String) = Database.watchPositionDao().findById(videoId)?.position

    fun getWatchPositionBlocking(videoId: String): Long? = runBlocking(Dispatchers.IO) {
        getWatchPosition(videoId)
    }

    suspend fun isVideoWatched(videoId: String, duration: Long): Boolean =
        withContext(Dispatchers.IO) {
            val position = getWatchPosition(videoId) ?: return@withContext false

            return@withContext isVideoWatched(position, duration)
        }

    fun isVideoWatched(positionMillis: Long, durationSeconds: Long?): Boolean {
        if (durationSeconds == null) return false

        val progress = positionMillis / 1000

        return durationSeconds - progress <= ABSOLUTE_WATCHED_THRESHOLD && progress >= RELATIVE_WATCHED_THRESHOLD * durationSeconds
    }

    suspend fun filterUnwatched(streams: List<StreamItem>): List<StreamItem> {
        return streams.filter {
            !isVideoWatched(it.url.orEmpty().toID(), it.duration ?: 0)
        }
    }

    suspend fun filterByStreamTypeAndWatchPosition(
        streams: List<StreamItem>,
        hideWatched: Boolean,
        showUpcoming: Boolean
    ): List<StreamItem> {
        val streamItems = streams.filter {
            if (!showUpcoming && it.isUpcoming) return@filter false

            val isVideo = !it.isShort && !it.isLive
            return@filter when {
                !ContentFilter.SHORTS.isEnabled && it.isShort -> false
                !ContentFilter.VIDEOS.isEnabled && isVideo -> false
                !ContentFilter.LIVESTREAMS.isEnabled && it.isLive -> false
                else -> true
            }
        }
        if (!hideWatched) return streamItems

        return filterUnwatched(streamItems)
    }
}
