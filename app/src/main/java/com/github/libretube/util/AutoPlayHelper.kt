package com.github.libretube.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoPlayHelper(
    private val playlistId: String
) {
    private val TAG = "AutoPlayHelper"

    private val playlistStreamIds = mutableListOf<String>()
    private var playlistNextPage: String? = null

    suspend fun getNextPlaylistVideoId(currentVideoId: String): String? {
        // if the playlists contain the video, then save the next video as next stream
        if (playlistStreamIds.contains(currentVideoId)) {
            val index = playlistStreamIds.indexOf(currentVideoId)
            // check whether there's a next video
            return if (index + 1 < playlistStreamIds.size) playlistStreamIds[index + 1]
            else getNextPlaylistVideoId(currentVideoId)
        } else if (playlistStreamIds.isEmpty() || playlistNextPage != null) {
            // fetch the next page of the playlist
            return withContext(Dispatchers.IO) {
                // fetch the playlists or its nextPage's videos
                val playlist =
                    if (playlistNextPage == null) RetrofitInstance.authApi.getPlaylist(playlistId)
                    else RetrofitInstance.authApi.getPlaylistNextPage(playlistId, playlistNextPage!!)
                // save the playlist urls to the list
                playlistStreamIds += playlist.relatedStreams!!.map { it.url.toID() }
                // save playlistNextPage for usage if video is not contained
                playlistNextPage = playlist.nextpage
                return@withContext getNextPlaylistVideoId(currentVideoId)
            }
        }
        // return null when no nextPage is found
        return null
    }
}
