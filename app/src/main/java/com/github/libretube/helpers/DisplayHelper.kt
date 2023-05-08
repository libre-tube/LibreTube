package com.github.libretube.helpers

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

object DisplayHelper {
    /**
     * Detect whether the device supports HDR as the ExoPlayer doesn't handle it properly
     * Returns false on and below SDK 24
     */
    fun supportsHdr(context: Context): Boolean {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            display.hdrCapabilities.supportedHdrTypes.isNotEmpty()
        } else {
            false
        }
    }
}
