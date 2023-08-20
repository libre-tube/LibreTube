package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.libretube.db.obj.SubscriptionGroup

@Dao()
interface SubscriptionGroupsDao {
    @Query("SELECT * FROM subscriptionGroups ORDER BY `index` ASC")
    suspend fun getAll(): List<SubscriptionGroup>

    @Query("SELECT EXISTS(SELECT * FROM subscriptionGroups WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroup(subscriptionGroup: SubscriptionGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptionGroups: List<SubscriptionGroup>)

    @Update
    suspend fun updateAll(subscriptionGroups: List<SubscriptionGroup>)

    @Query("DELETE FROM subscriptionGroups WHERE name = :name")
    suspend fun deleteGroup(name: String)
}
