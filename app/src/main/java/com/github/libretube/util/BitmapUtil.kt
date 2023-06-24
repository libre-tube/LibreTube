package com.github.libretube.util

import android.graphics.Bitmap
import com.github.libretube.obj.PreviewFrame

object BitmapUtil {
    /**
     * Cut off a new bitmap from the image that contains multiple preview thumbnails
     */
    fun cutBitmapFromPreviewFrame(bitmap: Bitmap, previewFrame: PreviewFrame): Bitmap {
        val heightPerFrame = bitmap.height / previewFrame.framesPerPageY
        val widthPerFrame = bitmap.width / previewFrame.framesPerPageX
        return Bitmap.createBitmap(
            bitmap,
            previewFrame.positionX * widthPerFrame,
            previewFrame.positionY * heightPerFrame,
            widthPerFrame,
            heightPerFrame,
        )
    }
}
