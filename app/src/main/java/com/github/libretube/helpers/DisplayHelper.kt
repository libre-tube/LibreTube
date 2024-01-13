package com.github.libretube.helpers

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

object DisplayHelper {
    /**
     * Detect whether the device supports HDR as the ExoPlayer doesn't handle it properly
     * Returns false below SDK 24
     */
    fun supportsHdr(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val display = ContextCompat.getDisplayOrDefault(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                display.isHdr
            } else {
                @Suppress("DEPRECATION")
                display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false
            }
        } else {
            false
        }
    }
}
