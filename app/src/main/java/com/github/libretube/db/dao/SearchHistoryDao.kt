package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.SearchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM searchHistoryItem ORDER BY rowid ASC")
    suspend fun getAll(): List<SearchHistoryItem>

    @Query("SELECT * FROM searchHistoryItem ORDER BY rowid ASC")
    fun getAllFlow(): Flow<List<SearchHistoryItem>>

    @Query("SELECT * FROM searchHistoryItem ORDER BY rowid DESC")
    fun getAllNewestFirstFlow(): Flow<List<SearchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistoryItem: SearchHistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(searchHistoryItems: List<SearchHistoryItem>)

    @Delete
    suspend fun delete(searchHistoryItem: SearchHistoryItem)

    @Query("DELETE FROM searchHistoryItem")
    suspend fun deleteAll()

    /**
     * Deletes the oldest rows, keeping the [keep] newest entries, in a single SQL statement.
     * Replaces the previous load-everything-then-delete-loop so history trimming does not read all
     * rows into memory on startup.
     */
    @Query(
        "DELETE FROM searchHistoryItem WHERE rowid IN (" +
            "SELECT rowid FROM searchHistoryItem ORDER BY rowid DESC LIMIT -1 OFFSET :keep)"
    )
    suspend fun deleteOldest(keep: Int)
}
