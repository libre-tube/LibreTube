package com.github.libretube.db

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.dao.WatchHistoryRow
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.LocalUserDataRepository.Companion.ABSOLUTE_WATCHED_THRESHOLD
import com.github.libretube.repo.LocalUserDataRepository.Companion.RELATIVE_WATCHED_THRESHOLD
import com.github.libretube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseHelper {
    private const val MAX_SEARCH_HISTORY_SIZE = 20

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

    suspend fun getWatchPosition(videoId: String) = runCatching {
        UserDataRepositoryHelper.userDataRepository
            .getFromWatchHistory(videoId)
    }.getOrNull()?.metadata?.positionMillis

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
