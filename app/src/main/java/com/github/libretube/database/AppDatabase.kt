package com.github.libretube.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.libretube.obj.WatchHistoryItem

@Database(entities = [WatchHistoryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
}
