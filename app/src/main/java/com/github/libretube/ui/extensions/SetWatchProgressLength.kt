package com.github.libretube.ui.extensions

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.awaitQuery

/**
 * Shows the already watched time under the video
 * @param videoId The id of the video to inspect
 * @param duration The duration of the video in seconds
 * @return Whether the video is already watched more than 90%
 */
fun View.setWatchProgressLength(videoId: String, duration: Long): Boolean {
    updateLayoutParams<ConstraintLayout.LayoutParams> {
        matchConstraintPercentWidth = 0f
    }
    visibility = View.GONE

    val progress = try {
        awaitQuery {
            Database.watchPositionDao().findById(videoId)?.position
        }
    } catch (e: Exception) {
        return false
    } // divide by 1000 to convert ms to seconds
        ?.toFloat()?.div(1000)

    if (progress == null || duration == 0L) {
        return false
    }

    updateLayoutParams<ConstraintLayout.LayoutParams> {
        matchConstraintPercentWidth = progress / duration.toFloat()
    }
    visibility = View.VISIBLE

    return progress / duration.toFloat() > 0.9
}
