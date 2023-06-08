package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watchHistoryItem")
    suspend fun getAll(): List<WatchHistoryItem>

    @Query("SELECT * FROM watchHistoryItem WHERE videoId LIKE :videoId LIMIT 1")
    suspend fun findById(videoId: String): WatchHistoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchHistoryItem: WatchHistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchHistoryItems: List<WatchHistoryItem>)

    @Delete
    suspend fun delete(watchHistoryItem: WatchHistoryItem)

    @Query("DELETE FROM watchHistoryItem WHERE videoId = :id")
    suspend fun deleteByVideoId(id: String)

    @Query("DELETE FROM watchHistoryItem")
    suspend fun deleteAll()
}
