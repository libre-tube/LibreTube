package com.github.libretube.database

import com.github.libretube.obj.Streams
import com.github.libretube.obj.WatchHistoryItem
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
        DatabaseHolder.database.watchHistoryDao().insertAll(watchHistoryItem)
    }
}
