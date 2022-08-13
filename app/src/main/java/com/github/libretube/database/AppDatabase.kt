package com.github.libretube.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.libretube.obj.CustomInstance
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.obj.WatchPosition

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchPosition::class,
        CustomInstance::class
    ],
    version = 3
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
     * Custom Instances
     */
    abstract fun customInstanceDao(): CustomInstanceDao
}
