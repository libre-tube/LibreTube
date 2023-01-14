package com.github.libretube.util

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.obj.PreviewFrame
import com.google.android.exoplayer2.ui.TimeBar

class SeekbarPreviewListener(
    private val previewFrames: List<PreviewFrames>,
    private val previewIv: ImageView,
    private val duration: Long
) : TimeBar.OnScrubListener {
    private var moving = false

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        moving = true

        processPreview(position)
    }

    /**
     * Show a preview of the scrubber position
     */
    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        moving = true

        processPreview(position)
    }

    /**
     * Hide the seekbar preview with a short delay
     */
    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        moving = false
        // animate the disappearance of the preview image
        previewIv.animate()
            .alpha(0f)
            .translationYBy(30f)
            .setDuration(200)
            .withEndAction {
                previewIv.visibility = View.GONE
                previewIv.translationY = previewIv.translationY - 30f
                previewIv.alpha = 1f
            }
            .start()
    }

    /**
     * Make a request to get the image frame and update its position
     */
    private fun processPreview(position: Long) {
        val previewFrame = getPreviewFrame(position) ?: return

        // update the offset of the preview image view
        updatePreviewX(position)

        val request = ImageRequest.Builder(previewIv.context)
            .data(previewFrame.previewUrl)
            .target {
                if (!moving) return@target
                val frame = cutOutBitmap(it.toBitmap(), previewFrame)
                previewIv.setImageBitmap(frame)
                previewIv.visibility = View.VISIBLE
            }
            .build()

        ImageHelper.imageLoader.enqueue(request)
    }

    /**
     * Get the preview frame according to the current position
     */
    private fun getPreviewFrame(position: Long): PreviewFrame? {
        var startPosition: Long = 0
        // get the frames with the best quality
        val frames = previewFrames.sortedBy { it.frameHeight }.lastOrNull()
        frames?.urls?.forEach { url ->
            // iterate over all available positions and find the one matching the current position
            for (y in 0 until frames.framesPerPageY!!) {
                for (x in 0 until frames.framesPerPageX!!) {
                    val endPosition = startPosition + frames.durationPerFrame!!.toLong()
                    if (position in startPosition until endPosition) {
                        return PreviewFrame(url, x, y, frames.framesPerPageX, frames.framesPerPageY)
                    }
                    startPosition = endPosition
                }
            }
        }
        return null
    }

    /**
     * Cut off a new bitmap from the image that contains multiple preview thumbnails
     */
    private fun cutOutBitmap(bitmap: Bitmap, previewFrame: PreviewFrame): Bitmap {
        val heightPerFrame = bitmap.height / previewFrame.framesPerPageY
        val widthPerFrame = bitmap.width / previewFrame.framesPerPageX
        return Bitmap.createBitmap(
            bitmap,
            previewFrame.positionX * widthPerFrame,
            previewFrame.positionY * heightPerFrame,
            widthPerFrame,
            heightPerFrame
        )
    }

    /**
     * Update the offset of the preview image to fit the current scrubber position
     */
    private fun updatePreviewX(position: Long) {
        val params = previewIv.layoutParams as MarginLayoutParams
        val parentWidth = (previewIv.parent as View).width
        // calculate the center-offset of the preview image view
        val offset = parentWidth * (position.toFloat() / duration.toFloat()) - previewIv.width / 2
        // normalize the offset to keep a minimum distance at left and right
        params.marginStart = normalizeOffset(
            offset.toInt(),
            MIN_PADDING,
            parentWidth - MIN_PADDING - previewIv.width
        )
        previewIv.layoutParams = params
    }

    /**
     * Normalize the offset to not overflow the screen
     */
    @Suppress("SameParameterValue")
    private fun normalizeOffset(offset: Int, min: Int, max: Int): Int {
        return maxOf(min, minOf(max, offset))
    }

    companion object {
        /**
         * The minimum start and end padding for the seekbar preview
         */
        const val MIN_PADDING = 20
    }
}
