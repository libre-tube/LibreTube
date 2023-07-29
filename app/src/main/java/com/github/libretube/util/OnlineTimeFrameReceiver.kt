package com.github.libretube.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.obj.PreviewFrame
import com.github.libretube.ui.interfaces.TimeFrameReceiver

class OnlineTimeFrameReceiver(
    private val context: Context,
    private val previewFrames: List<PreviewFrames>
): TimeFrameReceiver() {
    override suspend fun getFrameAtTime(position: Long): Bitmap? {
        val previewFrame = getPreviewFrame(previewFrames, position) ?: return null
        val drawable = ImageHelper.getImage(context, previewFrame.previewUrl).drawable ?: return null
        return cutBitmapFromPreviewFrame(drawable.toBitmap(), previewFrame)
    }

    /**
     * Cut off a new bitmap from the image that contains multiple preview thumbnails
     */
    private fun cutBitmapFromPreviewFrame(bitmap: Bitmap, previewFrame: PreviewFrame): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            previewFrame.positionX * previewFrame.frameWidth,
            previewFrame.positionY * previewFrame.frameHeight,
            previewFrame.frameWidth,
            previewFrame.frameHeight
        )
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