package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.SubscriptionGroup

@Dao()
interface SubscriptionGroupsDao {
    @Query("SELECT * FROM subscriptionGroups")
    suspend fun getAll(): List<SubscriptionGroup>

    @Query("SELECT EXISTS(SELECT * FROM subscriptionGroups WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroup(subscriptionGroup: SubscriptionGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptionGroups: List<SubscriptionGroup>)

    @Query("DELETE FROM subscriptionGroups WHERE name = :name")
    suspend fun deleteGroup(name: String)
}
