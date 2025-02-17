package com.github.libretube.repo

import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.PlaylistsHelper.MAX_CONCURRENT_IMPORT_CALLS
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.local.StreamsExtractor
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.extensions.parallelMap
import com.github.libretube.obj.PipedImportPlaylist

class LocalPlaylistsRepository: PlaylistRepository {
    override suspend fun getPlaylist(playlistId: String): Playlist {
        val relation = DatabaseHolder.Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }

        return Playlist(
            name = relation.playlist.name,
            description = relation.playlist.description,
            thumbnailUrl = relation.playlist.thumbnailUrl,
            videos = relation.videos.size,
            relatedStreams = relation.videos.map { it.toStreamItem() }
        )
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return DatabaseHolder.Database.localPlaylistsDao().getAll()
            .map {
                Playlists(
                    id = it.playlist.id.toString(),
                    name = it.playlist.name,
                    shortDescription = it.playlist.description,
                    thumbnail = it.playlist.thumbnailUrl,
                    videos = it.videos.size.toLong()
                )
            }
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        val localPlaylist = DatabaseHolder.Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }

        for (video in videos) {
            val localPlaylistItem = video.toLocalPlaylistItem(playlistId)
            // avoid duplicated videos in a playlist
            DatabaseHolder.Database.localPlaylistsDao()
                .deletePlaylistItemsByVideoId(playlistId, localPlaylistItem.videoId)

            // add the new video to the database
            DatabaseHolder.Database.localPlaylistsDao().addPlaylistVideo(localPlaylistItem)

            val playlist = localPlaylist.playlist
            if (playlist.thumbnailUrl.isEmpty()) {
                // set the new playlist thumbnail URL
                localPlaylistItem.thumbnailUrl?.let {
                    playlist.thumbnailUrl = it
                    DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
                }
            }
        }

        return true
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val playlist = DatabaseHolder.Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }.playlist
        playlist.name = newName
        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)

        return true
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        val playlist = DatabaseHolder.Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }.playlist
        playlist.description = newDescription
        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)

        return true
    }

    override suspend fun clonePlaylist(playlistId: String): String {
        val playlist = RetrofitInstance.api.getPlaylist(playlistId)
        val newPlaylist = createPlaylist(playlist.name ?: "Unknown name")

        PlaylistsHelper.addToPlaylist(newPlaylist, *playlist.relatedStreams.toTypedArray())

        var nextPage = playlist.nextpage
        while (nextPage != null) {
            nextPage = runCatching {
                RetrofitInstance.api.getPlaylistNextPage(playlistId, nextPage!!).apply {
                    PlaylistsHelper.addToPlaylist(newPlaylist, *relatedStreams.toTypedArray())
                }.nextpage
            }.getOrNull()
        }

        return playlistId
    }

    override suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        val transaction = DatabaseHolder.Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }
        DatabaseHolder.Database.localPlaylistsDao().removePlaylistVideo(
            transaction.videos[index]
        )
        // set a new playlist thumbnail if the first video got removed
        if (index == 0) {
            transaction.playlist.thumbnailUrl =
                transaction.videos.getOrNull(1)?.thumbnailUrl.orEmpty()
        }
        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(transaction.playlist)

        return true
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = createPlaylist(playlist.name!!)

            // if not logged in, all video information needs to become fetched manually
            // Only do so with `MAX_CONCURRENT_IMPORT_CALLS` videos at once to prevent performance issues
            for (videoIdList in playlist.videos.chunked(MAX_CONCURRENT_IMPORT_CALLS)) {
                val streams = videoIdList.parallelMap {
                    runCatching { StreamsExtractor.extractStreams(it) }
                        .getOrNull()
                        ?.toStreamItem(it)
                }.filterNotNull()

                PlaylistsHelper.addToPlaylist(playlistId, *streams.toTypedArray())
            }
        }
    }

    override suspend fun createPlaylist(playlistName: String): String {
        val playlist = LocalPlaylist(name = playlistName, thumbnailUrl = "")
        return DatabaseHolder.Database.localPlaylistsDao().createPlaylist(playlist).toString()
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        DatabaseHolder.Database.localPlaylistsDao().deletePlaylistById(playlistId)
        DatabaseHolder.Database.localPlaylistsDao().deletePlaylistItemsByPlaylistId(playlistId)

        return true
    }
}