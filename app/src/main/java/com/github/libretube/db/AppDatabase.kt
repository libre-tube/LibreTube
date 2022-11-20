package com.github.libretube.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.libretube.db.dao.CustomInstanceDao
import com.github.libretube.db.dao.LocalSubscriptionDao
import com.github.libretube.db.dao.LocalPlaylistsDao
import com.github.libretube.db.dao.PlaylistBookmarkDao
import com.github.libretube.db.dao.SearchHistoryDao
import com.github.libretube.db.dao.WatchHistoryDao
import com.github.libretube.db.dao.WatchPositionDao
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchPosition::class,
        SearchHistoryItem::class,
        CustomInstance::class,
        LocalSubscription::class,
        PlaylistBookmark::class,
        LocalPlaylist::class,
        LocalPlaylistItem::class
    ],
    version = 9,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9)
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

    /**
     * Bookmarked Playlists
     */
    abstract fun playlistBookmarkDao(): PlaylistBookmarkDao

    /**
     * Local playlists
     */
    abstract fun localPlaylistsDao(): LocalPlaylistsDao
}
