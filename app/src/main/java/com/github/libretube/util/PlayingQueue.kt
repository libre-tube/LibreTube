package com.github.libretube.util

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PlayingQueue {
    private val queue = mutableListOf<StreamItem>()
    private var currentStream: StreamItem? = null

    fun add(vararg streamItem: StreamItem) {
        streamItem.forEach {
            if (currentStream != it) {
                if (queue.contains(it)) queue.remove(it)
                queue.add(it)
            }
        }
    }

    fun addAsNext(streamItem: StreamItem) {
        if (currentStream == streamItem) return
        if (queue.contains(streamItem)) queue.remove(streamItem)
        queue.add(
            currentIndex() + 1,
            streamItem
        )
    }

    fun getNext(): String? {
        return try {
            queue[currentIndex() + 1].url?.toID()
        } catch (e: Exception) {
            null
        }
    }

    fun getPrev(): String? {
        val index = queue.indexOf(currentStream)
        return if (index > 0) queue[index - 1].url?.toID() else null
    }

    fun hasPrev(): Boolean {
        return queue.indexOf(currentStream) > 0
    }

    fun updateCurrent(streamItem: StreamItem) {
        currentStream = streamItem
        if (!contains(streamItem)) queue.add(streamItem)
    }

    fun isNotEmpty() = queue.isNotEmpty()

    fun isEmpty() = queue.isEmpty()

    fun clear() = queue.clear()

    fun size() = queue.size

    private fun currentIndex() = queue.indexOf(currentStream)

    fun contains(streamItem: StreamItem) = queue.any { it.url?.toID() == streamItem.url?.toID() }

    fun getStreams() = queue

    private fun fetchMoreFromPlaylist(playlistId: String, nextPage: String?) {
        var playlistNextPage: String? = nextPage
        CoroutineScope(Dispatchers.IO).launch {
            while (playlistNextPage != null) {
                RetrofitInstance.authApi.getPlaylistNextPage(
                    playlistId,
                    playlistNextPage!!
                ).apply {
                    add(
                        *this.relatedStreams.orEmpty().toTypedArray()
                    )
                    playlistNextPage = this.nextpage
                }
            }
        }
    }

    fun insertPlaylist(playlistId: String, newCurrentStream: StreamItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.authApi.getPlaylist(playlistId)
                add(
                    *response.relatedStreams
                        .orEmpty()
                        .toTypedArray()
                )
                updateCurrent(newCurrentStream)
                fetchMoreFromPlaylist(playlistId, response.nextpage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
