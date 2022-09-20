package com.github.libretube.db

import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.extensions.toID
import com.github.libretube.util.PreferenceHelper

object DatabaseHelper {
    fun addToWatchHistory(videoId: String, streams: com.github.libretube.api.obj.Streams) {
        val watchHistoryItem = WatchHistoryItem(
            videoId,
            streams.title,
            streams.uploadDate,
            streams.uploader,
            streams.uploaderUrl!!.toID(),
            streams.uploaderAvatar,
            streams.thumbnailUrl,
            streams.duration
        )
        Thread {
            Database.watchHistoryDao().insertAll(watchHistoryItem)
            val maxHistorySize =
                PreferenceHelper.getString(PreferenceKeys.WATCH_HISTORY_SIZE, "unlimited")
            if (maxHistorySize == "unlimited") return@Thread

            // delete the first watch history entry if the limit is reached
            val watchHistory = Database.watchHistoryDao().getAll()
            if (watchHistory.size > maxHistorySize.toInt()) {
                Database.watchHistoryDao()
                    .delete(watchHistory.first())
            }
        }.start()
    }

    fun removeFromWatchHistory(index: Int) {
        Thread {
            Database.watchHistoryDao().delete(
                Database.watchHistoryDao().getAll()[index]
            )
        }.start()
    }

    fun saveWatchPosition(videoId: String, position: Long) {
        val watchPosition = WatchPosition(
            videoId,
            position
        )
        Thread {
            Database.watchPositionDao().insertAll(watchPosition)
        }.start()
    }

    fun removeWatchPosition(videoId: String) {
        Thread {
            Database.watchPositionDao().findById(videoId)?.let {
                Database.watchPositionDao().delete(it)
            }
        }.start()
    }

    fun addToSearchHistory(searchHistoryItem: SearchHistoryItem) {
        Thread {
            Database.searchHistoryDao().insertAll(searchHistoryItem)
            val maxHistorySize = 20

            // delete the first watch history entry if the limit is reached
            val searchHistory = Database.searchHistoryDao().getAll()
            if (searchHistory.size > maxHistorySize) {
                Database.searchHistoryDao()
                    .delete(searchHistory.first())
            }
        }.start()
    }
}
