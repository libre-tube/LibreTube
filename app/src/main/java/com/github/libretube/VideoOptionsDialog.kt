package com.github.libretube

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.obj.Streams
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog with different options for a selected video.
 *
 * @param videoId The video id.
 */
class VideoOptionsDialog(private val videoId: String) : DialogFragment() {
    /**
     * List that stores the different menu options. In the future could be add more options here.
     */
    private val list = listOf("Background mode")

    /**
     * The response that gets when called the Api.
     */
    private lateinit var response: Streams

    /**
     * The [ExoPlayer] player. Followed tutorial [here](https://developer.android.com/codelabs/exoplayer-intro)
     */
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    /**
     * Dialog that returns a [MaterialAlertDialogBuilder] showing a menu of options.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.video_options_dialog_item,
                    list
                )
            ) { dialog, which ->
                // For now, this checks the position of the option with the position that is in the
                // list. I don't like it, but we will do like this for now.
                when (which) {
                    // This for example will be the "Background mode" option
                    0 -> {
                        lifecycleScope.launchWhenCreated {
                            // FIXME: For some reason I can't get the response
                            response = RetrofitInstance.api.getStreams(videoId)
                            initializePlayer()

                            player?.playWhenReady = playWhenReady
                            player?.seekTo(currentItem, playbackPosition)
                            player?.prepare()
                        }
                    }
                    else -> {
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    /**
     * Initializes the [Exoplayer] player with the [MediaItem].
     */
    private fun initializePlayer() {
        player = ExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                val mediaItem = MediaItem.fromUri(response.hls!!)
                exoPlayer.setMediaItem(mediaItem)
            }
    }

    /**
     * Releases the [ExoPlayer].
     */
    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }


    companion object {
        const val TAG = "VideoOptionsDialog"
    }
}
