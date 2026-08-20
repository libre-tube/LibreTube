package com.github.libretube.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.WatchHistoryItem

data class WatchHistoryPageItem(
    @Embedded val item: WatchHistoryItem,
    @ColumnInfo(name = "historyRowId") val rowId: Long,
    @ColumnInfo(name = "watchPosition") val watchPosition: Long?,
    @ColumnInfo(name = "isDownloaded") val isDownloaded: Boolean
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watchHistoryItem")
    suspend fun getAll(): List<WatchHistoryItem>

    @Query(
        """
        SELECT h.*, h.rowid AS historyRowId, p.position AS watchPosition,
            EXISTS(SELECT 1 FROM download AS d WHERE d.videoId = h.videoId) AS isDownloaded
        FROM watchHistoryItem AS h
        LEFT JOIN watchPosition AS p ON p.videoId = h.videoId
        WHERE h.rowid <= :cursor
        ORDER BY h.rowid DESC
        LIMIT :limit
        """
    )
    suspend fun getPage(limit: Int, cursor: Long): List<WatchHistoryPageItem>

    @Query(
        """
        SELECT h.*, h.rowid AS historyRowId, p.position AS watchPosition,
            EXISTS(SELECT 1 FROM download AS d WHERE d.videoId = h.videoId) AS isDownloaded
        FROM watchHistoryItem AS h
        LEFT JOIN watchPosition AS p ON p.videoId = h.videoId
        WHERE h.rowid <= :cursor AND CASE WHEN
            p.videoId IS NOT NULL AND
            h.duration IS NOT NULL AND
            h.duration - (p.position / 1000) <= :absoluteWatchedThreshold AND
            (p.position / 1000) >= :relativeWatchedThreshold * h.duration
            THEN 1 ELSE 0
        END = :watched
        ORDER BY h.rowid DESC
        LIMIT :limit
        """
    )
    suspend fun getFilteredPage(
        limit: Int,
        cursor: Long,
        watched: Int,
        absoluteWatchedThreshold: Float,
        relativeWatchedThreshold: Float
    ): List<WatchHistoryPageItem>

    @Query(
        """
        SELECT h.*, h.rowid AS historyRowId, p.position AS watchPosition,
            EXISTS(SELECT 1 FROM download AS d WHERE d.videoId = h.videoId) AS isDownloaded
        FROM watchHistoryItem AS h
        LEFT JOIN watchPosition AS p ON p.videoId = h.videoId
        WHERE h.videoId = :videoId
        LIMIT 1
        """
    )
    suspend fun getPageItem(videoId: String): WatchHistoryPageItem?

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
