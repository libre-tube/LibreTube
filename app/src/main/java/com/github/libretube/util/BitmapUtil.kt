package com.github.libretube.util

import android.graphics.Bitmap
import com.github.libretube.obj.PreviewFrame

object BitmapUtil {
    /**
     * Cut off a new bitmap from the image that contains multiple preview thumbnails
     */
    fun cutBitmapFromPreviewFrame(bitmap: Bitmap, previewFrame: PreviewFrame): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            previewFrame.positionX * previewFrame.frameWidth,
            previewFrame.positionY * previewFrame.frameHeight,
            previewFrame.frameWidth,
            previewFrame.frameHeight
        )
    }
}
