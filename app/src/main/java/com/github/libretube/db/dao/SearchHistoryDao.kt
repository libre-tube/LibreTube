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
    fun getAll(): List<SearchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg searchHistoryItem: SearchHistoryItem)

    @Delete
    fun delete(searchHistoryItem: SearchHistoryItem)

    @Query("DELETE FROM searchHistoryItem")
    fun deleteAll()
}
