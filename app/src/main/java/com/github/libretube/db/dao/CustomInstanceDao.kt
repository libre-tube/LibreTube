package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.libretube.db.obj.CustomInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomInstanceDao {
    @Query("SELECT * FROM customInstance ORDER BY name")
    suspend fun getAll(): List<CustomInstance>

    @Query("SELECT * FROM customInstance ORDER BY name")
    fun getAllFlow(): Flow<List<CustomInstance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customInstance: CustomInstance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customInstances: List<CustomInstance>)

    @Query("SELECT * FROM customInstance WHERE apiUrl = :apiUrl")
    suspend fun getByApiUrl(apiUrl: String): CustomInstance?

    @Delete
    suspend fun deleteCustomInstance(customInstance: CustomInstance)

    @Query("DELETE FROM customInstance")
    suspend fun deleteAll()
}
