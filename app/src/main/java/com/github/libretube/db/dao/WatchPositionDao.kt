package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchPosition

@Dao
interface WatchPositionDao {
    @Query("SELECT * FROM watchPosition")
    suspend fun getAll(): List<WatchPosition>

    @Query("SELECT * FROM watchPosition WHERE videoId LIKE :videoId LIMIT 1")
    suspend fun findById(videoId: String): WatchPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchPositions: List<WatchPosition>)

    @Query("DELETE FROM watchPosition")
    suspend fun deleteAll()
}
