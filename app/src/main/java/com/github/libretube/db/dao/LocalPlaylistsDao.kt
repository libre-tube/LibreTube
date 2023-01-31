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
    suspend fun getAll(): List<LocalPlaylistWithVideos>

    @Insert
    suspend fun createPlaylist(playlist: LocalPlaylist)

    @Update
    suspend fun updatePlaylist(playlist: LocalPlaylist)

    @Query("DELETE FROM localPlaylist WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Insert
    suspend fun addPlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Delete
    suspend fun removePlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Query("DELETE FROM localPlaylistItem WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItemsByPlaylistId(playlistId: String)

    @Query("DELETE FROM localPlaylistItem WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistItemsByVideoId(playlistId: String, videoId: String)
}
