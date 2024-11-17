package com.github.libretube.ui.extensions

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.helpers.ThemeHelper
import kotlinx.coroutines.runBlocking

/**
 * Shows the already watched time under the video
 * @param videoId The id of the video to inspect
 * @param duration The duration of the video in seconds
 */
fun View.setWatchProgressLength(videoId: String, duration: Long) {
    updateLayoutParams<ConstraintLayout.LayoutParams> {
        matchConstraintPercentWidth = 0f
    }
    var backgroundColor = ThemeHelper.getThemeColor(
        context,
        com.google.android.material.R.attr.colorPrimaryDark
    )
    // increase the brightness for better contrast in light mode
    if (!ThemeHelper.isDarkMode(context)) {
        backgroundColor = ColorUtils.blendARGB(backgroundColor, Color.WHITE, 0.4f)
    }
    setBackgroundColor(backgroundColor)
    isGone = true

    if (duration == 0L) {
        return
    }

    val progress = runCatching {
        runBlocking {
            Database.watchPositionDao().findById(videoId)?.position
                // divide by 1000 to convert ms to seconds
                ?.toFloat()?.div(1000)
        }
    }.getOrNull() ?: return

    updateLayoutParams<ConstraintLayout.LayoutParams> {
        matchConstraintPercentWidth = progress / duration.toFloat()
    }

    isVisible = true
}
