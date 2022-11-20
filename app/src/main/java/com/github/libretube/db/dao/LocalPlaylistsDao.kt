package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.LocalPlaylistWithVideos

@Dao
interface LocalPlaylistsDao {
    @Transaction
    @Query("SELECT * FROM LocalPlaylist")
    fun getAll(): List<LocalPlaylistWithVideos>

    @Insert
    fun createPlaylist(playlist: LocalPlaylist)

    @Update
    fun updatePlaylist(playlist: LocalPlaylist)

    @Delete
    fun deletePlaylist(playlist: LocalPlaylist)

    @Insert
    fun addPlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Delete
    fun removePlaylistVideo(playlistVideo: LocalPlaylistItem)
}
