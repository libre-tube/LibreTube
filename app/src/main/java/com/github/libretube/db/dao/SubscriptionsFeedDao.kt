package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.libretube.db.obj.SubscriptionsFeedItem

@Dao
interface SubscriptionsFeedDao {
    @Query("SELECT * FROM feedItem ORDER BY uploaded DESC")
    suspend fun getAll(): List<SubscriptionsFeedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feedItems: List<SubscriptionsFeedItem>)

    @Update
    suspend fun update(feedItem: SubscriptionsFeedItem)

    @Query("SELECT EXISTS (SELECT * FROM feedItem WHERE videoId = :videoId)")
    suspend fun contains(videoId: String): Boolean

    @Query("DELETE FROM feedItem WHERE (uploaded < :olderThan AND uploaded != -1)")
    suspend fun cleanUpOlderThan(olderThan: Long)

    @Query("DELETE FROM feedItem WHERE uploaderUrl NOT IN (:channelUrls)")
    suspend fun deleteAllExcept(channelUrls: List<String>)

    @Query("DELETE FROM feedItem")
    suspend fun deleteAll()
}