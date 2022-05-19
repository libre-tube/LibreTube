package com.github.libretube

import android.content.Context
import com.github.libretube.obj.Streams
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Loads the selected video audio in background mode with a notification area.
 *
 * Needs the [c], necessarily to build the [ExoPlayer] player, and [videoId] to get the video data.
 */
class BackgroundMode(private val c: Context, private val videoId: String) {
    /**
     * The response that gets when called the Api.
     */
    private var response: Streams? = null

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    private var player: ExoPlayer? = null
    private var playWhenReadyPlayer = true
    private var currentItem = 0
    private var playbackPosition = 0L

    /**
     * Initializes the [player] player with the [MediaItem].
     */
    private fun initializePlayer() {
        player = ExoPlayer.Builder(c)
            .build()
            .also { exoPlayer ->
                response?.let {
                    val mediaItem = MediaItem.fromUri(response!!.hls!!)
                    exoPlayer.setMediaItem(mediaItem)
                }
            }
    }

    /**
     * Releases the [player].
     */
    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReadyPlayer = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    /**
     * Gets the video data and prepares the [player].
     */
    fun playOnBackgroundMode() {
        runBlocking {
            val job = launch {
                response = RetrofitInstance.api.getStreams(videoId)
            }
            // Wait until the job is done, to load correctly later in the player
            job.join()

            initializePlayer()

            player?.apply {
                playWhenReady = playWhenReadyPlayer
                seekTo(currentItem, playbackPosition)
                prepare()
            }
        }
    }
}
