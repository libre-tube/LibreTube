package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchHistoryItem

data class WatchHistoryRow(
    @Embedded val item: WatchHistoryItem,
    val rowId: Long,
    val watchPosition: Long?
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watchHistoryItem")
    suspend fun getAll(): List<WatchHistoryItem>

    @Query(
        """
        SELECT h.*, h.rowid AS rowId, p.position AS watchPosition
        FROM watchHistoryItem AS h
        LEFT JOIN watchPosition AS p ON p.videoId = h.videoId
        WHERE h.rowid <= :cursor AND (
            :watched IS NULL OR (
                p.position IS NOT NULL
                AND h.duration IS NOT NULL
                AND h.duration - p.position / 1000 <= :absoluteWatchedThresholdSeconds
                AND p.position / 1000 >= :relativeWatchedThreshold * h.duration
            ) = :watched
        )
        ORDER BY h.rowid DESC
        LIMIT :limit
        """
    )
    suspend fun getPage(
        limit: Int,
        cursor: Long,
        watched: Boolean?,
        absoluteWatchedThresholdSeconds: Float,
        relativeWatchedThreshold: Float
    ): List<WatchHistoryRow>

    @Query("SELECT COUNT(videoId) FROM watchHistoryItem")
    suspend fun getSize(): Int

    @Query("SELECT * FROM watchHistoryItem WHERE videoId LIKE :videoId LIMIT 1")
    suspend fun findById(videoId: String): WatchHistoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchHistoryItem: WatchHistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchHistoryItems: List<WatchHistoryItem>)

    @Delete
    suspend fun delete(watchHistoryItem: WatchHistoryItem)

    @Query("SELECT * FROM watchHistoryItem LIMIT 1 OFFSET 0")
    suspend fun getOldest(): WatchHistoryItem

    @Query("DELETE FROM watchHistoryItem WHERE videoId = :id")
    suspend fun deleteByVideoId(id: String)

    @Query("DELETE FROM watchHistoryItem")
    suspend fun deleteAll()
}
