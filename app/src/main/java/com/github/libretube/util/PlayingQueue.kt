package com.github.libretube.util

object PlayingQueue {
    private val queue = mutableListOf<String>()
    private var currentVideoId: String? = null

    fun add(videoId: String) {
        if (currentVideoId == videoId) return
        if (queue.contains(videoId)) queue.remove(videoId)
        queue.add(videoId)
    }

    fun addAsNext(videoId: String) {
        if (currentVideoId == videoId) return
        if (queue.contains(videoId)) queue.remove(videoId)
        queue.add(
            queue.indexOf(currentVideoId) + 1,
            videoId
        )
    }

    fun getNext(): String? {
        return try {
            queue[currentIndex() + 1]
        } catch (e: Exception) {
            null
        }
    }

    fun getPrev(): String? {
        val index = queue.indexOf(currentVideoId)
        return if (index > 0) queue[index - 1] else null
    }

    fun hasPrev(): Boolean {
        return queue.indexOf(currentVideoId) > 0
    }

    fun updateCurrent(videoId: String) {
        currentVideoId = videoId
        queue.add(videoId)
    }

    fun isNotEmpty() = queue.isNotEmpty()

    fun clear() = queue.clear()

    fun currentIndex() = queue.indexOf(currentVideoId)

    fun contains(videoId: String) = queue.contains(videoId)

    fun containsBeforeCurrent(videoId: String): Boolean {
        return queue.contains(videoId) && queue.indexOf(videoId) < currentIndex()
    }
}
