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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object DatabaseHelper {
    private const val MAX_SEARCH_HISTORY_SIZE = 20
    private const val ABSOLUTE_WATCHED_THRESHOLD = 60.0f
    private const val RELATIVE_WATCHED_THRESHOLD = 0.75f

    // region Watch History

    suspend fun addToWatchHistory(item: WatchHistoryItem) =
        withContext(Dispatchers.IO) {
            Database.watchHistoryDao().insert(item)
        }

    suspend fun getWatchHistoryPage(page: Int, pageSize: Int): List<WatchHistoryItem> =
        withContext(Dispatchers.IO) {
            val dao = Database.watchHistoryDao()
            val totalSize = dao.getSize()
            val offset = totalSize - (pageSize * page)
            val limit = if (offset < 0) offset + pageSize else pageSize

            if (limit <= 0) emptyList()
            else dao.getN(limit, maxOf(offset, 0)).reversed()
        }

    // endregion

    // region Search History

    suspend fun addToSearchHistory(item: SearchHistoryItem) =
        withContext(Dispatchers.IO) {
            Database.searchHistoryDao().insert(item)
            trimSearchHistoryIfNeeded()
        }

    fun getSearchHistoryFlow(): Flow<List<SearchHistoryItem>> =
        Database.searchHistoryDao().getAllFlow().flowOn(Dispatchers.IO)

    private suspend fun trimSearchHistoryIfNeeded() {
        if (PreferenceHelper.getBoolean(PreferenceKeys.UNLIMITED_SEARCH_HISTORY, false)) return

        val history = Database.searchHistoryDao().getAll().toMutableList()
        while (history.size > MAX_SEARCH_HISTORY_SIZE) {
            Database.searchHistoryDao().delete(history.removeFirst())
        }
    }

    // endregion

    // region Watch Positions

    suspend fun getWatchPosition(videoId: String): Long? =
        withContext(Dispatchers.IO) {
            Database.watchPositionDao().findById(videoId)?.position
        }

    // endregion

    // region Watch Status Checks

    suspend fun isVideoWatched(videoId: String, duration: Long): Boolean =
        withContext(Dispatchers.IO) {
            val position = Database.watchPositionDao().findById(videoId)?.position ?: return@withContext false
            isVideoWatched(position, duration)
        }

    fun isVideoWatched(positionMillis: Long, durationSeconds: Long?): Boolean {
        if (durationSeconds == null) return false
        val progressSeconds = positionMillis / 1000
        return durationSeconds - progressSeconds <= ABSOLUTE_WATCHED_THRESHOLD &&
            progressSeconds >= RELATIVE_WATCHED_THRESHOLD * durationSeconds
    }

    suspend fun filterUnwatched(streams: List<StreamItem>): List<StreamItem> =
        streams.filter { !isVideoWatched(it.url.orEmpty().toID(), it.duration ?: 0) }

    suspend fun filterByWatchStatus(
        item: WatchHistoryItem,
        unfinished: Boolean = true
    ): Boolean = unfinished xor isVideoWatched(item.videoId, item.duration ?: 0)

    suspend fun filterByStreamTypeAndWatchPosition(
        streams: List<StreamItem>,
        hideWatched: Boolean,
        showUpcoming: Boolean
    ): List<StreamItem> {
        val filtered = streams.filter { stream ->
            if (!showUpcoming && stream.isUpcoming) return@filter false

            val isVideo = !stream.isShort && !stream.isLive
            when {
                !ContentFilter.SHORTS.isEnabled && stream.isShort -> false
                !ContentFilter.VIDEOS.isEnabled && isVideo -> false
                !ContentFilter.LIVESTREAMS.isEnabled && stream.isLive -> false
                else -> true
            }
        }

        return if (hideWatched) filterUnwatched(filtered) else filtered
    }

    // endregion
}
