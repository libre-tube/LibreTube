package com.github.libretube.helpers

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

object DisplayHelper {
    /**
     * Detect whether the device supports HDR as the ExoPlayer doesn't handle it properly
     * Returns false below SDK 24
     */
    fun supportsHdr(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && ContextCompat.getDisplayOrDefault(context).isHdr
}
