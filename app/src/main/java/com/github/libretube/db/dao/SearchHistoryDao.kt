package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.SearchHistoryItem

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM searchHistoryItem")
    suspend fun getAll(): List<SearchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(searchHistoryItems: List<SearchHistoryItem>)

    @Delete
    suspend fun delete(searchHistoryItem: SearchHistoryItem)

    @Query("DELETE FROM searchHistoryItem")
    suspend fun deleteAll()
}
