package com.github.libretube.util

import com.github.libretube.obj.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoPlayHelper(
    private val playlistId: String
) {
    private val TAG = "AutoPlayHelper"

    private val playlistStreamIds = mutableListOf<String>()
    private lateinit var playlist: Playlist
    private var playlistNextPage: String? = null

    suspend fun getNextPlaylistVideoId(currentVideoId: String): String? {
        // if the playlists contain the video, then save the next video as next stream
        if (playlistStreamIds.contains(currentVideoId)) {
            val index = playlistStreamIds.indexOf(currentVideoId)
            // check whether there's a next video
            return if (index < playlistStreamIds.size) playlistStreamIds[index + 1]
            else getNextPlaylistVideoId(currentVideoId)
        } else if (playlistStreamIds.isEmpty() || playlistNextPage != null) {
            // fetch the next page of the playlist
            return withContext(Dispatchers.IO) {
                // fetch the playlists videos
                playlist = RetrofitInstance.api.getPlaylist(playlistId)
                // save the playlist urls in the array
                playlistStreamIds += playlist.relatedStreams!!.map { it.url.toID() }
                // save playlistNextPage for usage if video is not contained
                playlistNextPage = playlist.nextpage
                return@withContext getNextPlaylistVideoId(currentVideoId)
            }
        }
        return null
    }
}
