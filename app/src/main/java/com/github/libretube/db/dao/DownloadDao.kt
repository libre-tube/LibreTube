package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadChapter
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.db.obj.DownloadPlaylist
import com.github.libretube.db.obj.DownloadPlaylistVideosCrossRef
import com.github.libretube.db.obj.DownloadPlaylistWithDownload
import com.github.libretube.db.obj.DownloadPlaylistWithDownloadWithItems
import com.github.libretube.db.obj.DownloadSponsorBlockSegment
import com.github.libretube.db.obj.DownloadWithItems

@Dao
interface DownloadDao {
    @Transaction
    @Query("SELECT * FROM download")
    suspend fun getAll(): List<DownloadWithItems>

    @Transaction
    @Query("SELECT * FROM download WHERE videoId = :videoId")
    suspend fun findById(videoId: String): DownloadWithItems?

    @Query("SELECT EXISTS (SELECT * FROM download WHERE videoId = :videoId)")
    suspend fun exists(videoId: String): Boolean

    @Query("SELECT * FROM downloaditem WHERE id = :id")
    suspend fun findDownloadItemById(id: Int): DownloadItem?

    @Query("DELETE FROM downloaditem WHERE id = :id")
    suspend fun deleteDownloadItemById(id: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDownload(download: Download)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDownloadChapter(downloadChapter: DownloadChapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadItem(downloadItem: DownloadItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownloadItem(downloadItem: DownloadItem)

    @Transaction
    @Delete
    suspend fun deleteDownload(download: Download)

    @Transaction
    @Query("SELECT * FROM downloadPlaylist")
    suspend fun getDownloadPlaylists(): List<DownloadPlaylistWithDownload>

    @Transaction
    @Query("SELECT * FROM downloadPlaylist WHERE playlistId = :playlistId")
    suspend fun getDownloadPlaylistById(playlistId: String): DownloadPlaylistWithDownload

    @Transaction
    @Query("SELECT * FROM downloadPlaylist WHERE playlistId = :playlistId")
    suspend fun getDownloadPlaylistByIdIncludingItems(playlistId: String): DownloadPlaylistWithDownloadWithItems

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(downloadPlaylist: DownloadPlaylist)

    /**
     * Connect a [DownloadPlaylist] to a [Download] to link the playlist to the video.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistVideoConnection(crossRef: DownloadPlaylistVideosCrossRef)

    @Suppress("DEPRECATION")
    suspend fun deletePlaylistIncludingVideoRefs(playlist: DownloadPlaylist) {
        deletePlaylistCrossRef(playlist.playlistId)
        deletePlaylist(playlist)
    }

    @Delete
    @Deprecated("Call deletePlaylistIncludingVideoRefs instead!")
    suspend fun deletePlaylist(playlist: DownloadPlaylist)

    @Query("DELETE FROM downloadplaylistvideoscrossref WHERE playlistId = :playlistId")
    @Deprecated("Call deletePlaylistIncludingVideoRefs instead!")
    suspend fun deletePlaylistCrossRef(playlistId: String)

    @Query("SELECT * FROM downloadplaylistvideoscrossref WHERE playlistId = :playlistId")
    suspend fun getVideoIdsFromPlaylist(playlistId: String): List<DownloadPlaylistVideosCrossRef>

    @Insert
    suspend fun insertSponsorBlockSegments(segments: List<DownloadSponsorBlockSegment>)
}
