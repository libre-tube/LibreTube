package com.github.libretube.ui.listeners

import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.TimeBar
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@UnstableApi
class SeekbarPreviewListener(
    private val timeFrameReceiver: TimeFrameReceiver,
    private val playerBinding: ExoStyledPlayerControlViewBinding,
    private val duration: Long,
    private val onScrub: (position: Long) -> Unit = {},
    private val onScrubEnd: (position: Long) -> Unit = {}
) : TimeBar.OnScrubListener {
    private var scrubInProgress = false
    private var lastPreviewPosition = Long.MAX_VALUE

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        scrubInProgress = true

        CoroutineScope(Dispatchers.IO).launch {
            processPreview(position)
        }
    }

    /**
     * Show a preview of the scrubber position
     */
    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        scrubInProgress = true

        playerBinding.seekbarPreviewPosition.text = DateUtils.formatElapsedTime(position / 1000)

        // minimum of five seconds of additional seeking in order to show a preview
        if ((lastPreviewPosition - position).absoluteValue < 5000) {
            updatePreviewX(position)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            processPreview(position)
        }

        runCatching {
            onScrub.invoke(position)
        }
    }

    /**
     * Hide the seekbar preview with a short delay
     */
    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        scrubInProgress = false
        // animate the disappearance of the preview image
        playerBinding.seekbarPreview.animate()
            .alpha(0f)
            .translationYBy(30f)
            .setDuration(200)
            .withEndAction {
                playerBinding.seekbarPreview.visibility = View.GONE
                playerBinding.seekbarPreview.translationY -= 30f
                playerBinding.seekbarPreview.alpha = 1f
            }
            .start()

        onScrubEnd.invoke(position)
    }

    /**
     * Make a request to get the image frame and update its position
     */
    private suspend fun processPreview(position: Long) {
        lastPreviewPosition = position

        val frame = timeFrameReceiver.getFrameAtTime(position)
        if (!scrubInProgress) return

        withContext(Dispatchers.Main) {
            playerBinding.seekbarPreviewImage.setImageBitmap(frame)
            playerBinding.seekbarPreview.isVisible = true
            updatePreviewX(position)
        }
    }

    /**
     * Update the offset of the preview image to fit the current scrubber position
     */
    private fun updatePreviewX(position: Long) {
        playerBinding.seekbarPreview.updateLayoutParams<MarginLayoutParams> {
            val parentWidth = (playerBinding.seekbarPreview.parent as View).width
            // calculate the center-offset of the preview image view
            val offset = parentWidth * (position.toFloat() / duration.toFloat()) -
                playerBinding.seekbarPreview.width / 2
            // normalize the offset to keep a minimum distance at left and right
            val maxPadding = parentWidth - MIN_PADDING - playerBinding.seekbarPreview.width
            marginStart = offset.toInt().coerceIn(MIN_PADDING, maxPadding)
        }
    }

    companion object {
        /**
         * The minimum start and end padding for the seekbar preview
         */
        const val MIN_PADDING = 20
    }
}
