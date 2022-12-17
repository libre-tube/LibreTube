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

    override fun onScrubStart(timeBar: TimeBar, position: Long) {}

    /**
     * Show a preview of the scrubber position
     */
    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        moving = true
        val preview = getPreviewFrame(position) ?: return
        updatePreviewX(position)

        val request = ImageRequest.Builder(previewIv.context)
            .data(preview.previewUrl)
            .target {
                if (!moving) return@target
                val bitmap = it.toBitmap()
                val heightPerFrame = bitmap.height / preview.framesPerPageY
                val widthPerFrame = bitmap.width / preview.framesPerPageX
                val frame = Bitmap.createBitmap(
                    bitmap,
                    preview.positionX * widthPerFrame,
                    preview.positionY * heightPerFrame,
                    widthPerFrame,
                    heightPerFrame
                )
                previewIv.setImageBitmap(frame)
                previewIv.visibility = View.VISIBLE
            }
            .build()

        ImageHelper.imageLoader.enqueue(request)
    }

    /**
     * Hide the seekbar preview with a short delay
     */
    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        moving = false
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
     * Get the preview frame according to the current position
     */
    private fun getPreviewFrame(position: Long): PreviewFrame? {
        var startPosition: Long = 0
        val frames = previewFrames.sortedBy { it.frameHeight }.lastOrNull()
        frames?.urls?.forEach { url ->
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

    private fun normalizeOffset(offset: Int, min: Int, max: Int): Int {
        return maxOf(min, minOf(max, offset))
    }

    companion object {
        const val MIN_PADDING = 20
    }
}
