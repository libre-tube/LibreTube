package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.LocalSubscription

@Dao
interface LocalSubscriptionDao {
    @Query("SELECT * FROM localSubscription")
    suspend fun getAll(): List<LocalSubscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(localSubscriptions: List<LocalSubscription>)

    @Delete
    suspend fun delete(localSubscription: LocalSubscription)

    @Query("SELECT EXISTS(SELECT * FROM localSubscription WHERE channelId = :channelId)")
    suspend fun includes(channelId: String): Boolean
}
