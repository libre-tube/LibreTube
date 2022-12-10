package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.libretube.db.obj.PlaylistBookmark

@Dao
interface PlaylistBookmarkDao {
    @Query("SELECT * FROM playlistBookmark")
    fun getAll(): List<PlaylistBookmark>

    @Query("SELECT * FROM playlistBookmark WHERE playlistId LIKE :playlistId LIMIT 1")
    fun findById(playlistId: String): PlaylistBookmark

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg bookmarks: PlaylistBookmark)

    @Delete
    fun delete(playlistBookmark: PlaylistBookmark)

    @Update
    fun update(playlistBookmark: PlaylistBookmark)

    @Query("DELETE FROM playlistBookmark WHERE playlistId = :playlistId")
    fun deleteById(playlistId: String)

    @Query("SELECT EXISTS(SELECT * FROM playlistBookmark WHERE playlistId= :playlistId)")
    fun includes(playlistId: String): Boolean

    @Query("DELETE FROM playlistBookmark")
    fun deleteAll()
}
