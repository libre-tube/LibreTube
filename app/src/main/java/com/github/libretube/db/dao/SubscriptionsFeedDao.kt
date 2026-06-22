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

    @Query("SELECT EXISTS (SELECT * FROM feedItem WHERE uploaderName = :uploaderName)")
    suspend fun anyVideoExists(uploaderName: String): Boolean

    @Query("DELETE FROM feedItem WHERE uploaded < :olderThan AND uploaded NOT IN ( SELECT uploaded FROM feedItem ORDER BY uploaded DESC LIMIT :minLimit)")
    suspend fun cleanUpOlderThan(olderThan: Long, minLimit : Int)

    @Query("DELETE FROM feedItem WHERE uploaderUrl = :channelUrl")
    suspend fun delete(channelUrl: String)

    @Query("DELETE FROM feedItem")
    suspend fun deleteAll()
}