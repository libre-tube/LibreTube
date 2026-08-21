package com.github.libretube.repo

import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.PlaylistsHelper.MAX_CONCURRENT_IMPORT_CALLS
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.dao.LocalPlaylistsDao
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.extensions.parallelMap
import com.github.libretube.obj.PipedImportPlaylist

class LocalPlaylistsRepository : PlaylistRepository {

    private val dao: LocalPlaylistsDao get() = DatabaseHolder.Database.localPlaylistsDao()

    override suspend fun getPlaylist(playlistId: String): Playlist {
        val relation = dao.getAll().first { it.playlist.id.toString() == playlistId }
        return Playlist(
            name = relation.playlist.name,
            description = relation.playlist.description,
            thumbnailUrl = relation.playlist.thumbnailUrl,
            videos = relation.videos.size,
            relatedStreams = relation.videos.map { it.toStreamItem() }
        )
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return dao.getAll().map {
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
        val localPlaylist = dao.getAll().first { it.playlist.id.toString() == playlistId }

        for (video in videos) {
            val localPlaylistItem = video.toLocalPlaylistItem(playlistId)

            val existingVideo = dao.getPlaylistVideo(playlistId, localPlaylistItem.videoId)
            if (existingVideo != null) {
                localPlaylistItem.id = existingVideo.id
                dao.updatePlaylistVideo(localPlaylistItem)
                continue
            }

            dao.addPlaylistVideo(localPlaylistItem)
            updateThumbnailIfNeeded(localPlaylist.playlist, localPlaylistItem.thumbnailUrl)
        }

        return true
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val playlist = dao.getAll().first { it.playlist.id.toString() == playlistId }.playlist
        playlist.name = newName
        dao.updatePlaylist(playlist)
        return true
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        val playlist = dao.getAll().first { it.playlist.id.toString() == playlistId }.playlist
        playlist.description = newDescription
        dao.updatePlaylist(playlist)
        return true
    }

    override suspend fun clonePlaylist(playlistId: String): String {
        val playlist = MediaServiceRepository.instance.getPlaylist(playlistId)
        val newPlaylistId = createPlaylist(playlist.name ?: "Unknown name")

        PlaylistsHelper.addToPlaylist(newPlaylistId, *playlist.relatedStreams.toTypedArray())

        var nextPage = playlist.nextpage
        while (nextPage != null) {
            nextPage = runCatching {
                MediaServiceRepository.instance.getPlaylistNextPage(playlistId, nextPage!!).apply {
                    PlaylistsHelper.addToPlaylist(newPlaylistId, *relatedStreams.toTypedArray())
                }.nextpage
            }.getOrNull()
        }

        return newPlaylistId
    }

    override suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        val playlistWithVideos = dao.getAll().first { it.playlist.id.toString() == playlistId }
        dao.removePlaylistVideo(playlistWithVideos.videos[index])

        if (index == 0) {
            playlistWithVideos.playlist.thumbnailUrl =
                playlistWithVideos.videos.getOrNull(1)?.thumbnailUrl.orEmpty()
        }
        dao.updatePlaylist(playlistWithVideos.playlist)

        return true
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = createPlaylist(playlist.name!!)

            for (videoIdList in playlist.videos.chunked(MAX_CONCURRENT_IMPORT_CALLS)) {
                val streams = videoIdList.parallelMap {
                    runCatching { MediaServiceRepository.instance.getStreams(it) }
                        .getOrNull()
                        ?.toStreamItem(it)
                }.filterNotNull()

                PlaylistsHelper.addToPlaylist(playlistId, *streams.toTypedArray())
            }
        }
    }

    override suspend fun createPlaylist(playlistName: String): String {
        val playlist = LocalPlaylist(name = playlistName, thumbnailUrl = "")
        return dao.createPlaylist(playlist).toString()
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        dao.deletePlaylistById(playlistId)
        dao.deletePlaylistItemsByPlaylistId(playlistId)
        return true
    }

    private suspend fun updateThumbnailIfNeeded(playlist: LocalPlaylist, thumbnailUrl: String?) {
        if (playlist.thumbnailUrl.isEmpty() && !thumbnailUrl.isNullOrEmpty()) {
            playlist.thumbnailUrl = thumbnailUrl
            dao.updatePlaylist(playlist)
        }
    }
}
