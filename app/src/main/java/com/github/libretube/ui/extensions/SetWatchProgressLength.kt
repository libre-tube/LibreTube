package com.github.libretube.ui.extensions

import android.graphics.Color
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.helpers.ThemeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows the already watched time under the video
 * @param videoId The id of the video to inspect
 * @param duration The duration of the video in seconds
 */
fun View.setWatchProgressLength(videoId: String, duration: Long) {
    isGone = true

    var backgroundColor = ThemeHelper.getThemeColor(
        context,
        com.google.android.material.R.attr.colorPrimaryVariant
    )
    // increase the brightness for better contrast in light mode
    if (!ThemeHelper.isDarkMode(context)) {
        backgroundColor = ColorUtils.blendARGB(backgroundColor, Color.WHITE, 0.4f)
    }
    setBackgroundColor(backgroundColor)

    // set corner-radius
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, 16f)
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        val progress = DatabaseHelper.getWatchPosition(videoId)?.div(1000)

        if (progress != null && progress == 0L) {
            withContext(Dispatchers.Main) {
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    matchConstraintPercentWidth = progress.toFloat()/ duration.toFloat()
                }
                isVisible = true
            }
        }
    }
}
