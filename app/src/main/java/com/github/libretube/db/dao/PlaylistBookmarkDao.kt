package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.libretube.db.obj.PlaylistBookmark

@Dao
interface PlaylistBookmarkDao {
    @Query("SELECT * FROM playlistBookmark")
    suspend fun getAll(): List<PlaylistBookmark>

    @Query("SELECT * FROM playlistBookmark WHERE playlistId = :playlistId LIMIT 1")
    suspend fun findById(playlistId: String): PlaylistBookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: PlaylistBookmark)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<PlaylistBookmark>)

    @Update
    suspend fun update(playlistBookmark: PlaylistBookmark)

    @Query("DELETE FROM playlistBookmark WHERE playlistId = :playlistId")
    suspend fun deleteById(playlistId: String)

    @Query("SELECT EXISTS(SELECT * FROM playlistBookmark WHERE playlistId= :playlistId)")
    suspend fun includes(playlistId: String): Boolean

    @Query("DELETE FROM playlistBookmark")
    suspend fun deleteAll()
}
