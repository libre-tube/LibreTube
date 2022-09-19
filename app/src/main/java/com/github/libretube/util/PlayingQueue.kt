package com.github.libretube.util

object PlayingQueue {
    val queue = mutableListOf<String>()

    fun clear() {
        queue.clear()
    }

    fun add(videoId: String) {
        queue.add(videoId)
    }

    fun playNext(currentVideoId: String, nextVideoId: String) {
        queue.add(
            queue.indexOf(currentVideoId),
            nextVideoId
        )
    }
}
