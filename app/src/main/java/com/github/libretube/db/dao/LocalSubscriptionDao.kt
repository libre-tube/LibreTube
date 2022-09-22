package com.github.libretube.db.dao

import androidx.room.*
import com.github.libretube.db.obj.LocalSubscription

@Dao
interface LocalSubscriptionDao {
    @Query("SELECT * FROM localSubscription")
    fun getAll(): List<LocalSubscription>

    @Query("SELECT * FROM localSubscription WHERE channelId LIKE :channelId LIMIT 1")
    fun findById(channelId: String): LocalSubscription

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg localSubscriptions: LocalSubscription)

    @Delete
    fun delete(localSubscription: LocalSubscription)

    @Query("DELETE FROM localSubscription")
    fun deleteAll()

    @Query("SELECT EXISTS(SELECT * FROM localSubscription WHERE channelId = :channelId)")
    fun includes(channelId: String): Boolean
}
