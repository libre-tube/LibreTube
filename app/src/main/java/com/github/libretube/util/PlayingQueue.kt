package com.github.libretube.util

import android.util.Log
import androidx.media3.common.Player
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.move
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
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

    var repeatMode: Int = Player.REPEAT_MODE_OFF

    fun clear() = queue.clear()

    /**
     * @param skipExisting Whether to skip the [streamItem] if it's already part of the queue
     */
    fun add(vararg streamItem: StreamItem, skipExisting: Boolean = false) {
        for (stream in streamItem) {
            if ((skipExisting && contains(stream)) || stream.title.isNullOrBlank()) continue

            queue.remove(stream)
            queue.add(stream)
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

    // return the next item, or if repeating enabled, the first one of the queue
    fun getNext(): String? {
        if (repeatMode != Player.REPEAT_MODE_ONE) {
            queue.getOrNull(currentIndex() + 1)?.url?.toID()?.let { return it }
        }

        return when (repeatMode) {
            Player.REPEAT_MODE_ALL -> queue.firstOrNull()?.url?.toID()
            Player.REPEAT_MODE_ONE -> currentStream?.url?.toID()
            else -> null
        }
    }

    // return the previous item, or if repeating enabled, the last one of the queue
    fun getPrev(): String? {
        if (repeatMode != Player.REPEAT_MODE_ONE) {
            queue.getOrNull(currentIndex() - 1)?.url?.toID()?.let { return it }
        }

        return when (repeatMode) {
            Player.REPEAT_MODE_ALL -> queue.lastOrNull()?.url?.toID()
            Player.REPEAT_MODE_ONE -> currentStream?.url?.toID()
            else -> null
        }
    }

    fun hasPrev() = getPrev() != null

    fun hasNext() = getNext() != null

    fun updateCurrent(streamItem: StreamItem, asFirst: Boolean = true) {
        currentStream = streamItem
        if (!contains(streamItem)) {
            val indexToAdd = if (asFirst) 0 else size()
            queue.add(indexToAdd, streamItem)
        }
    }

    fun isNotEmpty() = queue.isNotEmpty()

    fun isEmpty() = queue.isEmpty()

    fun size() = queue.size

    fun isLast() = currentIndex() == size() - 1

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

    /**
     * Adds a list of videos to the current queue while updating the position of the current stream
     * @param isMainList whether the videos are part of the list that initially has been used to
     * start the queue, either from a channel or playlist. If it's false, the current stream won't
     * be touched, since it's an independent list.
     */
    private fun addToQueueAsync(
        streams: List<StreamItem>,
        currentStreamItem: StreamItem? = null,
        isMainList: Boolean = true
    ) {
        if (!isMainList) {
            add(*streams.toTypedArray())
            return
        }
        val currentStream = currentStreamItem ?: this.currentStream
        // if the stream already got added to the queue earlier, although it's not yet
        // been found in the playlist, remove it and re-add it later
        var reAddStream = true
        if (currentStream != null && streams.includes(currentStream)) {
            queue.removeAll { it.url?.toID() == currentStream.url?.toID() }
            reAddStream = false
        }
        // add all new stream items to the queue
        add(*streams.toTypedArray())

        if (currentStream != null && reAddStream) {
            // re-add the stream to the end of the queue
            updateCurrent(currentStream, false)
        }
    }

    private fun fetchMoreFromPlaylist(playlistId: String, nextPage: String?, isMainList: Boolean) {
        var playlistNextPage = nextPage
        scope.launch(Dispatchers.IO) {
            while (playlistNextPage != null) {
                RetrofitInstance.authApi.getPlaylistNextPage(playlistId, playlistNextPage!!).run {
                    addToQueueAsync(relatedStreams, isMainList = isMainList)
                    playlistNextPage = this.nextpage
                }
            }
        }
    }

    fun insertPlaylist(playlistId: String, newCurrentStream: StreamItem?) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val playlist = PlaylistsHelper.getPlaylist(playlistId)
                val isMainList = newCurrentStream != null
                addToQueueAsync(playlist.relatedStreams, newCurrentStream, isMainList)
                if (playlist.nextpage == null) return@launch
                fetchMoreFromPlaylist(playlistId, playlist.nextpage, isMainList)
            }
        }
    }

    private fun fetchMoreFromChannel(channelId: String, nextPage: String?) {
        var channelNextPage = nextPage
        scope.launch(Dispatchers.IO) {
            runCatching {
                while (channelNextPage != null) {
                    RetrofitInstance.api.getChannelNextPage(channelId, nextPage!!).run {
                        addToQueueAsync(relatedStreams)
                        channelNextPage = this.nextpage
                    }
                }
            }
        }
    }

    private fun insertChannel(channelId: String, newCurrentStream: StreamItem) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val channel = RetrofitInstance.api.getChannel(channelId)
                addToQueueAsync(channel.relatedStreams, newCurrentStream)
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

    fun updateQueue(
        streamItem: StreamItem,
        playlistId: String?,
        channelId: String?,
        relatedStreams: List<StreamItem> = emptyList()
    ) {
        if (playlistId != null) {
            insertPlaylist(playlistId, streamItem)
        } else if (channelId != null) {
            insertChannel(channelId, streamItem)
        } else if (relatedStreams.isNotEmpty()) {
            insertRelatedStreams(relatedStreams)
        }
        updateCurrent(streamItem)
    }

    fun insertRelatedStreams(streams: List<StreamItem>) {
        if (!PlayerHelper.autoInsertRelatedVideos) return
        add(*streams.toTypedArray(), skipExisting = true)
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
        repeatMode = Player.REPEAT_MODE_OFF
        onQueueTapListener = {}
    }

    private fun List<StreamItem>.includes(item: StreamItem) = any {
        it.url?.toID() == item.url?.toID()
    }
}
