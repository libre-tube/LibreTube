package com.github.libretube.util

import android.content.Context
import android.graphics.Bitmap
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.obj.PreviewFrame
import com.github.libretube.ui.interfaces.TimeFrameReceiver

class OnlineTimeFrameReceiver(
    private val context: Context,
    private val previewFrames: List<PreviewFrames>
) : TimeFrameReceiver() {
    override suspend fun getFrameAtTime(position: Long): Bitmap? {
        val previewFrame = getPreviewFrame(previewFrames, position) ?: return null
        val bitmap = ImageHelper.getImage(context, previewFrame.previewUrl) ?: return null
        return cutBitmapFromPreviewFrame(bitmap, previewFrame)
    }

    /**
     * Cut off a new bitmap from the image that contains multiple preview thumbnails
     */
    private fun cutBitmapFromPreviewFrame(bitmap: Bitmap, previewFrame: PreviewFrame): Bitmap? {
        val offsetX = previewFrame.positionX * previewFrame.frameWidth
        val offsetY = previewFrame.positionY * previewFrame.frameHeight

        return runCatching {
            Bitmap.createBitmap(
                bitmap,
                offsetX,
                offsetY,
                previewFrame.frameWidth,
                previewFrame.frameHeight
            )
        }.getOrNull()
    }

    /**
     * Get the preview frame according to the current position
     */
    private fun getPreviewFrame(previewFrames: List<PreviewFrames>, position: Long): PreviewFrame? {
        var startPosition = 0L
        // get the frames with the best quality
        val frames = previewFrames.maxByOrNull { it.frameHeight }
        frames?.urls?.forEach { url ->
            // iterate over all available positions and find the one matching the current position
            for (y in 0 until frames.framesPerPageY) {
                for (x in 0 until frames.framesPerPageX) {
                    val endPosition = startPosition + frames.durationPerFrame
                    if (position in startPosition until endPosition) {
                        return PreviewFrame(url, x, y, frames.frameWidth, frames.frameHeight)
                    }
                    startPosition = endPosition
                }
            }
        }
        return null
    }
}
