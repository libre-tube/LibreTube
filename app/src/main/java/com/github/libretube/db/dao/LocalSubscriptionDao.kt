package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.LocalSubscription

@Dao
interface LocalSubscriptionDao {
    @Query("SELECT * FROM localSubscription")
    suspend fun getAll(): List<LocalSubscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localSubscription: LocalSubscription)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(localSubscriptions: List<LocalSubscription>)

    @Query("DELETE FROM localSubscription WHERE channelId = :channelId")
    suspend fun deleteById(channelId: String)

    /**
     * Get all channels that DO NOT contain any meta info (such as their name) yet.
     */
    @Query("SELECT * FROM localSubscription WHERE name IS NULL")
    suspend fun getChannelsWithoutMetaInfo(): List<LocalSubscription>

    @Query("SELECT EXISTS(SELECT * FROM localSubscription WHERE channelId = :channelId)")
    suspend fun includes(channelId: String): Boolean
}
