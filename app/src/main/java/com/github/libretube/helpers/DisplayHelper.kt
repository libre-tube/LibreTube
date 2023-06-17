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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
                .hdrCapabilities.supportedHdrTypes.isNotEmpty()
    }
}
