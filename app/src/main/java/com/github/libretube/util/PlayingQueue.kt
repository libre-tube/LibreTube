package com.github.libretube.util

import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toID

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
        queue.add(streamItem)
    }

    fun isNotEmpty() = queue.isNotEmpty()

    fun isEmpty() = queue.isEmpty()

    fun clear() = queue.clear()

    fun size() = queue.size

    private fun currentIndex() = queue.indexOf(currentStream)

    fun contains(streamItem: StreamItem) = queue.contains(streamItem)

    fun getStreams() = queue
}
