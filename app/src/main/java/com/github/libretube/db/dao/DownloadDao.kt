package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.db.obj.DownloadWithItems

@Dao
interface DownloadDao {
    @Transaction
    @Query("SELECT * FROM download")
    fun getAll(): List<DownloadWithItems>

    @Transaction
    @Query("SELECT * FROM download WHERE videoId = :videoId")
    fun findById(videoId: String): DownloadWithItems

    @Query("SELECT * FROM downloaditem WHERE id = :id")
    fun findDownloadItemById(id: Int): DownloadItem

    @Query("SELECT * FROM downloadItem WHERE path = :path")
    fun findDownloadItemByFilePath(path: String): DownloadItem

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDownload(download: Download)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDownloadItem(downloadItem: DownloadItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateDownload(download: Download)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateDownloadItem(downloadItem: DownloadItem)

    @Transaction
    @Delete
    fun deleteDownload(download: Download)

    @Delete
    fun deleteDownloadItem(downloadItem: DownloadItem)

    @Query("DELETE FROM downloadItem WHERE videoId = :videoId")
    fun deleteDownloadItemsByVideoId(videoId: String)
}
