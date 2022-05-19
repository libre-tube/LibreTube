package com.github.libretube

import android.content.Context
import com.github.libretube.obj.Streams
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Loads the selected video audio in background mode with a notification area.
 */
class BackgroundMode {
    /**
     * The response that gets when called the Api.
     */
    private var response: Streams? = null

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    private var player: ExoPlayer? = null
    private var playWhenReadyPlayer = true

    /**
     * Initializes the [player] player with the [MediaItem].
     */
    private fun initializePlayer(c: Context) {
        if (player == null) player = ExoPlayer.Builder(c).build()
        setMediaItem()
    }

    /**
     * Releases the [player].
     */
    private fun releasePlayer() {
        player?.release()
        player = null
    }

    /**
     * Sets the [MediaItem] with the [response] into the [player].
     */
    private fun setMediaItem() {
        response?.let {
            val mediaItem = MediaItem.fromUri(response!!.hls!!)
            player?.setMediaItem(mediaItem)
        }
    }

    /**
     * Gets the video data and prepares the [player].
     */
    fun playOnBackgroundMode(c: Context, videoId: String) {
        runBlocking {
            val job = launch {
                response = RetrofitInstance.api.getStreams(videoId)
            }
            // Wait until the job is done, to load correctly later in the player
            job.join()

            initializePlayer(c)

            player?.apply {
                playWhenReady = playWhenReadyPlayer
                prepare()
            }
        }
    }

    /**
     * Creates a singleton of this class, to not create a new [player] every time.
     */
    companion object {
        private var INSTANCE: BackgroundMode? = null

        fun getInstance(): BackgroundMode {
            if (INSTANCE == null) INSTANCE = BackgroundMode()
            return INSTANCE!!
        }
    }
}
