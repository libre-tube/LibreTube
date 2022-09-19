package com.github.libretube.util

object PlayingQueue {
    private val queue = mutableListOf<String>()
    private var currentVideoId: String? = null

    fun clear() {
        queue.clear()
    }

    fun add(videoId: String) {
        if (currentVideoId == videoId) return
        if (queue.contains(videoId)) queue.remove(videoId)
        queue.add(videoId)
    }

    fun playNext(videoId: String) {
        if (currentVideoId == videoId) return
        if (queue.contains(videoId)) queue.remove(videoId)
        queue.add(
            queue.indexOf(currentVideoId) + 1,
            videoId
        )
    }

    fun getNext(): String? {
        val currentIndex = queue.indexOf(currentVideoId)
        return if (currentIndex >= queue.size) {
            null
        } else {
            queue[currentIndex + 1]
        }
    }

    fun getPrev(): String {
        return queue[
            queue.indexOf(currentVideoId) - 1
        ]
    }

    fun hasPrev(): Boolean {
        val currentIndex = queue.indexOf(currentVideoId)
        return queue.size > currentIndex + 1
    }

    fun contains(videoId: String): Boolean {
        return queue.contains(videoId)
    }

    fun containsBefore(videoId: String): Boolean {
        return queue.contains(videoId) && queue.indexOf(videoId) < queue.indexOf(currentVideoId)
    }

    fun updateCurrent(videoId: String) {
        currentVideoId = videoId
        if (!contains(videoId)) add(videoId)
    }

    fun isNotEmpty() = queue.isNotEmpty()
}
