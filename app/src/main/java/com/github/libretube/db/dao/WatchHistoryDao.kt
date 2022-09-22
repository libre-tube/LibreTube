package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchHistoryItem

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watchHistoryItem")
    fun getAll(): List<WatchHistoryItem>

    @Query("SELECT * FROM watchHistoryItem WHERE videoId LIKE :videoId LIMIT 1")
    fun findById(videoId: String): WatchHistoryItem

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg watchHistoryItems: WatchHistoryItem)

    @Delete
    fun delete(watchHistoryItem: WatchHistoryItem)

    @Query("DELETE FROM watchHistoryItem")
    fun deleteAll()
}
