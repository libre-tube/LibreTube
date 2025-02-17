package com.github.libretube.util

import androidx.media3.common.Player
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.local.StreamsExtractor
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.move
import com.github.libretube.extensions.runCatchingIO
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import kotlinx.coroutines.Job
import java.util.Collections

object PlayingQueue {
    // queue is a synchronized list to be safely accessible from different coroutine threads
    private val queue = Collections.synchronizedList(mutableListOf<StreamItem>())
    private var currentStream: StreamItem? = null

    private val queueJobs = mutableListOf<Job>()

    // wrapper around PlayerHelper#repeatMode for compatibility
    var repeatMode: Int
        get() = PlayerHelper.repeatMode
        set(value) {
            PlayerHelper.repeatMode = value
        }

    fun clear() {
        queueJobs.forEach {
            it.cancel()
        }
        queueJobs.clear()
        queue.clear()
    }

    /**
     * @param skipExisting Whether to skip the [streamItem] if it's already part of the queue
     */
    fun add(vararg streamItem: StreamItem, skipExisting: Boolean = false) = synchronized(queue) {
        for (stream in streamItem) {
            if ((skipExisting && contains(stream)) || stream.title.isNullOrBlank()) continue

            queue.remove(stream)
            queue.add(stream)
        }
    }

    fun addAsNext(streamItem: StreamItem) = synchronized(queue) {
        if (currentStream == streamItem) return
        if (queue.contains(streamItem)) queue.remove(streamItem)
        queue.add(currentIndex() + 1, streamItem)
    }

    // return the next item, or if repeating enabled and no video left, the first one of the queue
    fun getNext(): String? = synchronized(queue) {
        val nextItem = queue.getOrNull(currentIndex() + 1)
        if (nextItem != null) return nextItem.url?.toID()

        if (repeatMode == Player.REPEAT_MODE_ALL) return queue.firstOrNull()?.url?.toID()

        return null
    }

    // return the previous item, or if repeating enabled and no video left, the last one of the queue
    fun getPrev(): String? = synchronized(queue) {
        val prevItem = queue.getOrNull(currentIndex() - 1)
        if (prevItem != null) return prevItem.url?.toID()

        if (repeatMode == Player.REPEAT_MODE_ALL) return queue.lastOrNull()?.url?.toID()

        return null
    }

    fun hasPrev() = getPrev() != null

    fun hasNext() = getNext() != null

    fun updateCurrent(streamItem: StreamItem, asFirst: Boolean = true) = synchronized(queue) {
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

    fun currentIndex(): Int = synchronized(queue) {
        return queue.indexOfFirst {
            it.url?.toID() == currentStream?.url?.toID()
        }.takeIf { it >= 0 } ?: 0
    }

    fun getCurrent(): StreamItem? = currentStream

    fun contains(streamItem: StreamItem) = synchronized(queue) {
        queue.any { it.url?.toID() == streamItem.url?.toID() }
    }

    // only returns a copy of the queue, no write access
    fun getStreams() = queue.toList()

    fun setStreams(streams: List<StreamItem>) = synchronized(queue) {
        queue.clear()
        queue.addAll(streams)
    }

    fun remove(index: Int) = synchronized(queue) {
        queue.removeAt(index)
        return@synchronized
    }

    fun move(from: Int, to: Int) = synchronized(queue) {
        queue.move(from, to)
    }

    /**
     * Adds a list of videos to the current queue while updating the position of the current stream
     * @param isMainList whether the videos are part of the list that initially has been used to
     * start the queue, either from a channel or playlist. If it's false, the current stream won't
     * be touched, since it's an independent list.
     */
    private fun addToQueueAsync(
        streams: List<StreamItem>, currentStreamItem: StreamItem? = null, isMainList: Boolean = true
    ) = synchronized(queue) {
        if (!isMainList) {
            add(*streams.toTypedArray())
            return
        }
        val currentStream = currentStreamItem ?: this.currentStream
        // if the stream already got added to the queue earlier, although it's not yet
        // been found in the playlist, remove it and re-add it later
        var reAddStream = true
        if (currentStream != null && streams.any { it.url?.toID() == currentStream.url?.toID() }) {
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

    private suspend fun fetchMoreFromPlaylist(
        playlistId: String,
        nextPage: String?,
        isMainList: Boolean
    ) {
        var playlistNextPage = nextPage
        while (playlistNextPage != null) {
            RetrofitInstance.authApi.getPlaylistNextPage(playlistId, playlistNextPage).run {
                addToQueueAsync(relatedStreams, isMainList = isMainList)
                playlistNextPage = this.nextpage
            }
        }
    }

    fun insertPlaylist(playlistId: String, newCurrentStream: StreamItem?) = runCatchingIO {
        val playlist = PlaylistsHelper.getPlaylist(playlistId)
        val isMainList = newCurrentStream != null
        addToQueueAsync(playlist.relatedStreams, newCurrentStream, isMainList)
        if (playlist.nextpage == null) return@runCatchingIO
        fetchMoreFromPlaylist(playlistId, playlist.nextpage, isMainList)
    }.let { queueJobs.add(it) }

    private suspend fun fetchMoreFromChannel(channelId: String, nextPage: String?) {
        var channelNextPage = nextPage
        var pageIndex = 1
        while (channelNextPage != null && pageIndex < 10) {
            RetrofitInstance.api.getChannelNextPage(channelId, channelNextPage).run {
                addToQueueAsync(relatedStreams)
                channelNextPage = this.nextpage
                pageIndex++
            }
        }
    }

    private fun insertChannel(channelId: String, newCurrentStream: StreamItem) = runCatchingIO {
        val channel = RetrofitInstance.api.getChannel(channelId)
        addToQueueAsync(channel.relatedStreams, newCurrentStream)
        if (channel.nextpage == null) return@runCatchingIO
        fetchMoreFromChannel(channelId, channel.nextpage)
    }.let { queueJobs.add(it) }

    fun insertByVideoId(videoId: String) = runCatchingIO {
        val streams = StreamsExtractor.extractStreams(videoId.toID())
        add(streams.toStreamItem(videoId))
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

        // don't add new videos to the queue if the user chose to repeat only the current queue
        if (isLast() && repeatMode == Player.REPEAT_MODE_ALL) return

        add(*streams.filter { !it.isLive }.toTypedArray(), skipExisting = true)
    }
}
