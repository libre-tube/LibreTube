package com.github.libretube.ui.extensions

import android.os.Build
import android.widget.ImageView

/**
 * Attempts to set the tooltip for the ImageView.
 * If the OS does not support tooltips, this function will have no effect.
 * @param tooltip The tooltip of the image
 */
fun ImageView.trySetTooltip(tooltip: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        tooltipText = tooltip
    }
}
