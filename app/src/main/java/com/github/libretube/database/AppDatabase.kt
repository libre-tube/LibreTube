package com.github.libretube.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.libretube.obj.CustomInstance
import com.github.libretube.obj.SearchHistoryItem
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.obj.WatchPosition

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchPosition::class,
        SearchHistoryItem::class,
        CustomInstance::class
    ],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Watch History
     */
    abstract fun watchHistoryDao(): WatchHistoryDao

    /**
     * Watch Positions
     */
    abstract fun watchPositionDao(): WatchPositionDao

    /**
     * Search History
     */
    abstract fun searchHistoryDao(): SearchHistoryDao

    /**
     * Custom Instances
     */
    abstract fun customInstanceDao(): CustomInstanceDao
}
