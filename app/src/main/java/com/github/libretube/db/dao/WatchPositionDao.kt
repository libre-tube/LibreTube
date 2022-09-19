package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchPosition

@Dao
interface WatchPositionDao {
    @Query("SELECT * FROM watchPosition")
    fun getAll(): List<WatchPosition>

    @Query("SELECT * FROM watchPosition WHERE videoId LIKE :videoId LIMIT 1")
    fun findById(videoId: String): WatchPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg watchPositions: WatchPosition)

    @Delete
    fun delete(watchPosition: WatchPosition)

    @Query("DELETE FROM watchPosition")
    fun deleteAll()
}
