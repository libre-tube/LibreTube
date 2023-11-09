package com.github.libretube.db

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DatabaseHelper {
    private const val MAX_SEARCH_HISTORY_SIZE = 20

    suspend fun addToWatchHistory(videoId: String, streams: Streams) = addToWatchHistory(
        videoId,
        streams.toStreamItem(videoId)
    )
    suspend fun addToWatchHistory(videoId: String, stream: StreamItem) =
        withContext(Dispatchers.IO) {
            val watchHistoryItem = WatchHistoryItem(
                videoId,
                stream.title,
                Instant.fromEpochMilliseconds(stream.uploaded)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                stream.uploaderName,
                stream.uploaderUrl?.toID(),
                stream.uploaderAvatar,
                stream.thumbnail,
                stream.duration
            )
            Database.watchHistoryDao().insert(watchHistoryItem)
            val maxHistorySize = PreferenceHelper.getString(
                PreferenceKeys.WATCH_HISTORY_SIZE,
                "100"
            )
            if (maxHistorySize == "unlimited") {
                return@withContext
            }

            // delete the first watch history entry if the limit is reached
            val watchHistory = Database.watchHistoryDao().getAll()
            if (watchHistory.size > maxHistorySize.toInt()) {
                Database.watchHistoryDao().delete(watchHistory.first())
            }
        }

    suspend fun addToSearchHistory(searchHistoryItem: SearchHistoryItem) {
        Database.searchHistoryDao().insert(searchHistoryItem)

        if (PreferenceHelper.getBoolean(PreferenceKeys.UNLIMITED_SEARCH_HISTORY, false)) return

        // delete the first watch history entry if the limit is reached
        val searchHistory = Database.searchHistoryDao().getAll().toMutableList()

        while (searchHistory.size > MAX_SEARCH_HISTORY_SIZE) {
            Database.searchHistoryDao().delete(searchHistory.first())
            searchHistory.removeFirst()
        }
    }

    suspend fun filterUnwatched(streams: List<StreamItem>): List<StreamItem> {
        return streams.filter {
            withContext(Dispatchers.IO) {
                val historyItem = Database.watchPositionDao()
                    .findById(it.url.orEmpty().toID()) ?: return@withContext true
                val progress = historyItem.position / 1000
                val duration = it.duration ?: 0
                // show video only in feed when watched less than 90%
                progress < 0.9f * duration
            }
        }
    }

    suspend fun filterUnwatchedHistory(streams: List<WatchHistoryItem>): List<WatchHistoryItem> {
        return streams.filter {
            withContext(Dispatchers.IO) {
                val historyItem = Database.watchPositionDao()
                    .findById(it.videoId) ?: return@withContext true
                val progress = historyItem.position / 1000
                val duration = it.duration ?: 0
                // show video only in feed when watched less than 90%
                progress < 0.9f * duration
            }
        }
    }

    suspend fun filterWatchedHistory(streams: List<WatchHistoryItem>): List<WatchHistoryItem> {
        return streams.filter {
            withContext(Dispatchers.IO) {
                val historyItem = Database.watchPositionDao()
                    .findById(it.videoId) ?: return@withContext true
                val progress = historyItem.position / 1000
                val duration = it.duration ?: 0
                // show video only in feed when watched less than 90%
                progress > 0.9f * duration
            }
        }
    }
}
