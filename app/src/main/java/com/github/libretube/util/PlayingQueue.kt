package com.github.libretube.util

object PlayingQueue {
    val queue = mutableListOf<String>()
    val currentVideoId: String? = null

    fun clear() {
        queue.clear()
    }

    fun add(videoId: String) {
        queue.add(videoId)
    }

    fun playNext(nextVideoId: String) {
        queue.add(
            queue.indexOf(currentVideoId),
            nextVideoId
        )
    }

    fun getNext(): String? {
        val currentIndex = queue.indexOf(currentVideoId)
        return if (currentIndex > queue.size) null
        else queue[currentIndex + 1]
    }
}
