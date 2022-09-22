package com.github.libretube.util

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.extensions.toID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoPlayHelper(
    private val playlistId: String?
) {

    private val playlistStreamIds = mutableListOf<String>()
    private var playlistNextPage: String? = null

    /**
     * get the id of the next video to be played
     */
    suspend fun getNextVideoId(
        currentVideoId: String,
        relatedStreams: List<com.github.libretube.api.obj.StreamItem>?
    ): String? {
        return if (playlistId == null) {
            getNextTrendingVideoId(
                relatedStreams
            )
        } else {
            getNextPlaylistVideoId(
                currentVideoId
            )
        }
    }

    /**
     * get the id of the next related video
     */
    private fun getNextTrendingVideoId(
        relatedStreams: List<com.github.libretube.api.obj.StreamItem>?
    ): String? {
        // don't play a video if it got played before already
        if (relatedStreams == null || relatedStreams.isEmpty()) return null
        var index = 0
        var nextStreamId: String? = null
        while (nextStreamId == null || PlayingQueue.containsBefore(nextStreamId)) {
            nextStreamId = relatedStreams[index].url!!.toID()
            if (index + 1 < relatedStreams.size) {
                index += 1
            } else {
                break
            }
        }
        return nextStreamId
    }

    /**
     * get the videoId of the next video in a playlist
     */
    private suspend fun getNextPlaylistVideoId(currentVideoId: String): String? {
        // if the playlists contain the video, then save the next video as next stream
        if (playlistStreamIds.contains(currentVideoId)) {
            val index = playlistStreamIds.indexOf(currentVideoId)
            // check whether there's a next video
            return if (index + 1 < playlistStreamIds.size) {
                playlistStreamIds[index + 1]
            } else if (playlistNextPage == null) {
                null
            } else {
                getNextPlaylistVideoId(currentVideoId)
            }
        } else if (playlistStreamIds.isEmpty() || playlistNextPage != null) {
            // fetch the next page of the playlist
            return withContext(Dispatchers.IO) {
                // fetch the playlists or its nextPage's videos
                val playlist =
                    if (playlistNextPage == null) {
                        RetrofitInstance.authApi.getPlaylist(playlistId!!)
                    } else {
                        RetrofitInstance.authApi.getPlaylistNextPage(
                            playlistId!!,
                            playlistNextPage!!
                        )
                    }
                // save the playlist urls to the list
                playlistStreamIds += playlist.relatedStreams!!.map { it.url!!.toID() }
                // save playlistNextPage for usage if video is not contained
                playlistNextPage = playlist.nextpage
                return@withContext getNextPlaylistVideoId(currentVideoId)
            }
        }
        // return null when no nextPage is found
        return null
    }
}
