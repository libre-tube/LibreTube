package com.github.libretube.db

import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseHelper {
    private const val MAX_SEARCH_HISTORY_SIZE = 20

    suspend fun addToWatchHistory(videoId: String, streams: Streams) = withContext(Dispatchers.IO) {
        val watchHistoryItem = WatchHistoryItem(
            videoId,
            streams.title,
            streams.uploadDate,
            streams.uploader,
            streams.uploaderUrl.toID(),
            streams.uploaderAvatar,
            streams.thumbnailUrl,
            streams.duration,
        )
        Database.watchHistoryDao().insert(watchHistoryItem)
        val maxHistorySize = PreferenceHelper.getString(PreferenceKeys.WATCH_HISTORY_SIZE, "100")
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
}
