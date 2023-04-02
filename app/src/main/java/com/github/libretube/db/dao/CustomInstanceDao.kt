package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.CustomInstance

@Dao
interface CustomInstanceDao {
    @Query("SELECT * FROM customInstance")
    suspend fun getAll(): List<CustomInstance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customInstance: CustomInstance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customInstances: List<CustomInstance>)

    @Query("DELETE FROM customInstance")
    suspend fun deleteAll()
}
