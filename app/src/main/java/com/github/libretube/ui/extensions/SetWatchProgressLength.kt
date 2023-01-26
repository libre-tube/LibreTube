package com.github.libretube.ui.extensions

import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.awaitQuery

/**
 * Shows the already watched time under the video
 * @param videoId The id of the video to inspect
 * @param duration The duration of the video in seconds
 * @return Whether the video is already watched more than 90%
 */
fun View?.setWatchProgressLength(videoId: String, duration: Long): Boolean {
    val view = this!!

    val progress = try {
        awaitQuery {
            Database.watchPositionDao().findById(videoId)?.position
        }
    } catch (e: Exception) {
        return false
    } // divide by 1000 to convert ms to seconds
        ?.toFloat()?.div(1000)

    if (progress == null || duration == 0L) {
        view.visibility = View.GONE
        return false
    }

    view.viewTreeObserver
        .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@setWatchProgressLength.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val fullWidth = (parent as LinearLayout).width
                val newWidth = fullWidth * (progress / duration.toFloat())
                view.updateLayoutParams {
                    width = newWidth.toInt()
                }
                view.visibility = View.VISIBLE
            }
        })

    return progress / duration.toFloat() > 0.9
}
