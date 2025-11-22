package com.github.libretube.ui.listeners

import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.TimeBar
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.extensions.dpToPx
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@UnstableApi
class SeekbarPreviewListener(
    private val timeFrameReceiver: TimeFrameReceiver,
    private val playerBinding: ExoStyledPlayerControlViewBinding,
    private val duration: Long
) : TimeBar.OnScrubListener {
    private var lastPreviewPosition = Long.MAX_VALUE
    private var lastPreviewPositionX = Float.MAX_VALUE
    private var prevProcessPreviewJob : Job? = null

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        processPreview(position)
    }

    /**
     * Show a preview of the scrubber position
     */
    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        playerBinding.seekbarPreviewPosition.text = DateUtils.formatElapsedTime(position / 1000)
        updatePreviewX(position)

        // minimum of five seconds of additional seeking in order to show a preview
        if (abs(lastPreviewPosition - position) < 5000L) {
            return
        }

        // minimum of distance in pixels of additional seeking in order to show a preview
        if (abs(lastPreviewPositionX - getCenterOffset(position)) < MIN_SEEK_DISTANCE_PX) {
            return
        }

        playerBinding.previewProgressIndicator.isVisible = true
        processPreview(position)
    }

    /**
     * Hide the seekbar preview with a short delay
     */
    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        prevProcessPreviewJob?.cancel()
        prevProcessPreviewJob = null

        // animate the disappearance of the preview image
        playerBinding.seekbarPreview.animate()
            .alpha(0f)
            .translationYBy(30f)
            .setDuration(200)
            .withEndAction {
                playerBinding.seekbarPreview.isGone = true
                playerBinding.seekbarPreview.translationY -= 30f
                playerBinding.seekbarPreview.alpha = 1f
            }
            .start()
    }

    /**
     * Make a request to get the image frame and update its position
     */
    private fun processPreview(position: Long) {
        prevProcessPreviewJob?.cancel()
        prevProcessPreviewJob = CoroutineScope(Dispatchers.IO).launch {
            lastPreviewPosition = position
            lastPreviewPositionX = getCenterOffset(position)

            val frame = timeFrameReceiver.getFrameAtTime(position)
            withContext(Dispatchers.Main) {
                playerBinding.previewProgressIndicator.isInvisible = true
                playerBinding.seekbarPreviewImage.setImageBitmap(frame)
                playerBinding.seekbarPreview.isVisible = true
            }
        }
    }

    /**
     * Update the offset of the preview image to fit the current scrubber position
     */
    private fun updatePreviewX(position: Long) {
        playerBinding.seekbarPreview.updateLayoutParams<MarginLayoutParams> {
            val parentWidth = (playerBinding.seekbarPreview.parent as View).width
            val offset = getCenterOffset(position)
            // normalize the offset to keep a minimum distance at left and right
            val maxPadding = parentWidth - MIN_PADDING - playerBinding.seekbarPreview.width
            marginStart = offset.toInt().coerceIn(MIN_PADDING, maxPadding)
        }
    }

    /**
     * Calculate the center-offset of the preview image view
     */
    private fun getCenterOffset(position: Long): Float =
        playerBinding.seekbarPreview.parent.let { parent ->
            val seekbarPreviewWidth = playerBinding.seekbarPreview.width
            parent as View
            parent.width * (position.toFloat() / duration.toFloat()) - seekbarPreviewWidth / 2
        }

    companion object {
        /**
         * The minimum start and end padding for the seekbar preview
         */
        const val MIN_PADDING = 20

        val MIN_SEEK_DISTANCE_PX = 8f.dpToPx()
    }
}
