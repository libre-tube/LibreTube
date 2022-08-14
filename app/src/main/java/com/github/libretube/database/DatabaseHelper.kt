package com.github.libretube.database

import com.github.libretube.obj.Streams
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.obj.WatchPosition
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
            DatabaseHolder.database.watchHistoryDao().insertAll(watchHistoryItem)
            val maxHistorySize =
                PreferenceHelper.getString(PreferenceKeys.WATCH_HISTORY_SIZE, "unlimited")
            if (maxHistorySize == "unlimited") return@Thread

            // delete the first watch history entry if the limit is reached
            val watchHistory = DatabaseHolder.database.watchHistoryDao().getAll()
            if (watchHistory.size > maxHistorySize.toInt()) {
                DatabaseHolder.database.watchHistoryDao()
                    .delete(watchHistory.first())
            }
        }.start()
    }

    fun removeFromWatchHistory(index: Int) {
        Thread {
            DatabaseHolder.database.watchHistoryDao().delete(
                DatabaseHolder.database.watchHistoryDao().getAll()[index]
            )
        }.start()
    }

    fun saveWatchPosition(videoId: String, position: Long) {
        val watchPosition = WatchPosition(
            videoId,
            position
        )
        Thread {
            DatabaseHolder.database.watchPositionDao().insertAll(watchPosition)
        }.start()
    }

    fun removeWatchPosition(videoId: String) {
        Thread {
            DatabaseHolder.database.watchPositionDao().delete(
                DatabaseHolder.database.watchPositionDao().findById(videoId)
            )
        }.start()
    }
}
