package com.github.libretube.db

import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.obj.Streams
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.toID

object DatabaseHelper {
    fun addToWatchHistory(videoId: String, streams: Streams) {
        val watchHistoryItem = WatchHistoryItem(
            videoId,
            streams.title,
            streams.uploadDate,
            streams.uploader,
            streams.uploaderUrl.toID(),
            streams.uploaderAvatar,
            streams.thumbnailUrl,
            streams.duration
        )
        Thread {
            DatabaseHolder.db.watchHistoryDao().insertAll(watchHistoryItem)
            val maxHistorySize =
                PreferenceHelper.getString(PreferenceKeys.WATCH_HISTORY_SIZE, "unlimited")
            if (maxHistorySize == "unlimited") return@Thread

            // delete the first watch history entry if the limit is reached
            val watchHistory = DatabaseHolder.db.watchHistoryDao().getAll()
            if (watchHistory.size > maxHistorySize.toInt()) {
                DatabaseHolder.db.watchHistoryDao()
                    .delete(watchHistory.first())
            }
        }.start()
    }

    fun removeFromWatchHistory(index: Int) {
        Thread {
            DatabaseHolder.db.watchHistoryDao().delete(
                DatabaseHolder.db.watchHistoryDao().getAll()[index]
            )
        }.start()
    }

    fun saveWatchPosition(videoId: String, position: Long) {
        val watchPosition = WatchPosition(
            videoId,
            position
        )
        Thread {
            DatabaseHolder.db.watchPositionDao().insertAll(watchPosition)
        }.start()
    }

    fun removeWatchPosition(videoId: String) {
        Thread {
            DatabaseHolder.db.watchPositionDao().delete(
                DatabaseHolder.db.watchPositionDao().findById(videoId)
            )
        }.start()
    }

    fun addToSearchHistory(searchHistoryItem: SearchHistoryItem) {
        Thread {
            DatabaseHolder.db.searchHistoryDao().insertAll(searchHistoryItem)
            val maxHistorySize = 20

            // delete the first watch history entry if the limit is reached
            val searchHistory = DatabaseHolder.db.searchHistoryDao().getAll()
            if (searchHistory.size > maxHistorySize) {
                DatabaseHolder.db.searchHistoryDao()
                    .delete(searchHistory.first())
            }
        }.start()
    }
}
