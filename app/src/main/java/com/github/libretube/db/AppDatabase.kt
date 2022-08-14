package com.github.libretube.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.libretube.db.dao.CustomInstanceDao
import com.github.libretube.db.dao.LocalSubscriptionDao
import com.github.libretube.db.dao.SearchHistoryDao
import com.github.libretube.db.dao.WatchHistoryDao
import com.github.libretube.db.dao.WatchPositionDao
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchPosition::class,
        SearchHistoryItem::class,
        CustomInstance::class,
        LocalSubscription::class
    ],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 7, to = 8)
    ]
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

    /**
     * Local Subscriptions
     */
    abstract fun localSubscriptionDao(): LocalSubscriptionDao
}
