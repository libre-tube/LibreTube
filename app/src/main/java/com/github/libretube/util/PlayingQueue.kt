package com.github.libretube.util

import android.util.Log
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.move
import com.github.libretube.extensions.toID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PlayingQueue {
    private val queue = mutableListOf<StreamItem>()
    private var currentStream: StreamItem? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Listener that gets called when the user selects an item from the queue
     */
    private var onQueueTapListener: (StreamItem) -> Unit = {}

    var repeatQueue: Boolean = false

    fun clear() = queue.clear()

    fun add(vararg streamItem: StreamItem) {
        for (stream in streamItem) {
            if (currentStream?.url?.toID() == stream.url?.toID() || stream.title.isNullOrBlank()) continue
            // remove if already present
            queue.remove(stream)
            queue.add(stream)
        }
    }

    fun addAsNext(streamItem: StreamItem) {
        if (currentStream == streamItem) return
        if (queue.contains(streamItem)) queue.remove(streamItem)
        queue.add(
            currentIndex() + 1,
            streamItem,
        )
    }

    fun getNext(): String? = queue.getOrNull(currentIndex() + 1)?.url?.toID()
        ?: queue.firstOrNull()?.url?.toID()?.takeIf { repeatQueue }

    fun getPrev(): String? = queue.getOrNull(currentIndex() - 1)?.url?.toID()

    fun hasPrev(): Boolean {
        return currentIndex() > 0
    }

    fun hasNext(): Boolean {
        return currentIndex() + 1 < size()
    }

    fun updateCurrent(streamItem: StreamItem) {
        currentStream = streamItem
        if (!contains(streamItem)) queue.add(0, streamItem)
    }

    fun isNotEmpty() = queue.isNotEmpty()

    fun isEmpty() = queue.isEmpty()

    fun size() = queue.size

    fun currentIndex(): Int = queue.indexOfFirst {
        it.url?.toID() == currentStream?.url?.toID()
    }.takeIf { it >= 0 } ?: 0

    fun getCurrent(): StreamItem? = currentStream

    fun contains(streamItem: StreamItem) = queue.any { it.url?.toID() == streamItem.url?.toID() }

    // only returns a copy of the queue, no write access
    fun getStreams() = queue.toList()

    fun setStreams(streams: List<StreamItem>) {
        queue.clear()
        queue.addAll(streams)
    }

    fun remove(index: Int) = queue.removeAt(index)

    fun move(from: Int, to: Int) = queue.move(from, to)

    private fun fetchMoreFromPlaylist(playlistId: String, nextPage: String?) {
        var playlistNextPage: String? = nextPage
        scope.launch {
            while (playlistNextPage != null) {
                RetrofitInstance.authApi.getPlaylistNextPage(
                    playlistId,
                    playlistNextPage!!,
                ).apply {
                    add(
                        *this.relatedStreams.toTypedArray(),
                    )
                    playlistNextPage = this.nextpage
                }
            }
        }
    }

    fun insertPlaylist(playlistId: String, newCurrentStream: StreamItem) {
        scope.launch {
            try {
                val playlist = PlaylistsHelper.getPlaylist(playlistId)
                add(*playlist.relatedStreams.toTypedArray())
                updateCurrent(newCurrentStream)
                if (playlist.nextpage == null) return@launch
                fetchMoreFromPlaylist(playlistId, playlist.nextpage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchMoreFromChannel(channelId: String, nextPage: String?) {
        var channelNextPage: String? = nextPage
        scope.launch {
            while (channelNextPage != null) {
                RetrofitInstance.api.getChannelNextPage(channelId, nextPage!!).apply {
                    add(*relatedStreams.toTypedArray())
                    channelNextPage = this.nextpage
                }
            }
        }
    }

    fun insertChannel(channelId: String, newCurrentStream: StreamItem) {
        scope.launch {
            runCatching {
                val channel = RetrofitInstance.api.getChannel(channelId)
                add(*channel.relatedStreams.toTypedArray())
                updateCurrent(newCurrentStream)
                if (channel.nextpage == null) return@launch
                fetchMoreFromChannel(channelId, channel.nextpage)
            }
        }
    }

    fun insertByVideoId(videoId: String) {
        scope.launch {
            runCatching {
                val streams = RetrofitInstance.api.getStreams(videoId.toID())
                add(streams.toStreamItem(videoId))
            }
        }
    }

    fun onQueueItemSelected(index: Int) {
        try {
            val streamItem = queue[index]
            updateCurrent(streamItem)
            onQueueTapListener.invoke(streamItem)
        } catch (e: Exception) {
            Log.e("Queue on tap", "lifecycle already ended")
        }
    }

    fun setOnQueueTapListener(listener: (StreamItem) -> Unit) {
        onQueueTapListener = listener
    }

    fun resetToDefaults() {
        repeatQueue = false
        onQueueTapListener = {}
    }
}
