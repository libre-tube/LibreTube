package com.github.libretube.db.obj

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.nio.file.Path

@Entity(tableName = "downloadPlaylist")
data class DownloadPlaylist(
    @PrimaryKey(autoGenerate = false)
    val playlistId: String = "",
    val title: String = "",
    val thumbnailPath: Path? = null,
    val description: String? = null,
)

@Entity(primaryKeys = ["playlistId", "videoId"])
data class DownloadPlaylistVideosCrossRef(
    val playlistId: String,
    val videoId: String,
)

data class DownloadPlaylistWithDownload(
    @Embedded val downloadPlaylist: DownloadPlaylist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "videoId",
        associateBy = Junction(DownloadPlaylistVideosCrossRef::class)
    )
    val downloadVideos: List<Download>
)

/**
 * Additionally contains information about all download items (e.g. video/audio files)
 */
data class DownloadPlaylistWithDownloadWithItems(
    @Embedded val downloadPlaylist: DownloadPlaylist,
    @Relation(
        entity = Download::class,
        parentColumn = "playlistId",
        entityColumn = "videoId",
        associateBy = Junction(DownloadPlaylistVideosCrossRef::class)
    )
    val downloadVideos: List<DownloadWithItems>
)