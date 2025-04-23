package com.github.libretube.db

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ContentFilter
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
            val maxHistorySize = PreferenceHelper.getString(
                PreferenceKeys.WATCH_HISTORY_SIZE,
                "100"
            )
            if (maxHistorySize == "unlimited") {
                return@withContext
            }

            // delete the first watch history entry if the limit is reached
            val historySize = Database.watchHistoryDao().getSize()
            if (historySize > maxHistorySize.toInt()) {
                Database.watchHistoryDao().delete(Database.watchHistoryDao().getOldest())
            }
        }

    suspend fun getWatchHistoryPage(page: Int, pageSize: Int): List<WatchHistoryItem> {
        val watchHistoryDao = Database.watchHistoryDao()
        val historySize = watchHistoryDao.getSize()

        if (historySize < pageSize * (page - 1)) return emptyList()

        val offset = historySize - (pageSize * page)
        val limit = if (offset < 0) {
            offset + pageSize
        } else {
            pageSize
        }
        return watchHistoryDao.getN(limit, maxOf(offset, 0)).reversed()
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

    suspend fun filterByWatchStatus(
        watchHistoryItem: WatchHistoryItem,
        unfinished: Boolean = true
    ): Boolean {
        return unfinished xor isVideoWatched(watchHistoryItem.videoId, watchHistoryItem.duration ?: 0)
    }

    suspend fun filterByStatusAndWatchPosition(
        streams: List<StreamItem>,
        hideWatched: Boolean
    ): List<StreamItem> {
        val streamItems = streams.filter {
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
